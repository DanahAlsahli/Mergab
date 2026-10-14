# قواعد الصلاحيات المقترحة للباك إند (Spring Security)

هذي نفس القواعد اللي يطبقها الباك إند الوهمي في الفرونت (`mock-server.js`)، عشان لما تربطون يشتغل كل شي بنفس الطريقة.
ما أضفتها للمشروع، وهي هنا كمرجع للي بيشتغل على السكيورتي.

## 1) User لازم يطبّق UserDetails
عندكم TODO في `User.java`. الدور يصير authority باسمه مثل ما هو: `ADMIN` أو `ORGANIZER` أو `VISITOR`.
```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(this.role));
}
@Override public String getUsername() { return this.email; }   // الدخول بالإيميل
```
و `MyUserDetailsService` يدوّر المستخدم بالإيميل.

## 2) endpoint يرجّع المستخدم الحالي ودوره
الفرونت يناديه بعد الدخول عشان يعرف وين يوجّه المستخدم.
```java
// UserController
@GetMapping("/me")
public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
    return ResponseEntity.status(200).body(user);
}
```

## 3) SecurityConfig
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // صفحات الفرونت نفسها (الحماية الحقيقية على الـ API تحت)
                .requestMatchers("/", "/*.html", "/assets/**", "/visitor/**", "/organizer/**", "/admin/**").permitAll()

                // مفتوح للكل
                .requestMatchers(HttpMethod.POST, "/api/v1/visitor/register").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/zone/get", "/api/v1/zone/get-zone-by-name/**",
                        "/api/v1/zone-connection/get",
                        "/api/v1/expo-event/get", "/api/v1/expo-event/get-by-id/**", "/api/v1/expo-event/get-by-zone/**"
                ).permitAll()

                // أي مستخدم داخل
                .requestMatchers("/api/v1/user/me").authenticated()

                // الأدمن: إدارة الحسابات
                .requestMatchers("/api/v1/user/**", "/api/v1/organizer/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/visitor/get-all").hasAuthority("ADMIN")

                // الزائر: حسابه (تأكدوا في الـ Service إن id = id المستخدم الداخل)
                .requestMatchers("/api/v1/visitor/**").hasAnyAuthority("VISITOR", "ADMIN")

                // المنظم: بيانات المعرض والمحاكاة
                .requestMatchers(
                        "/api/v1/zone/**", "/api/v1/zone-connection/**", "/api/v1/expo-event/**",
                        "/api/v1/simulation/**", "/api/v1/simulation-snapshot/**",
                        "/api/v1/intervention/**", "/api/v1/attendance/**"
                ).hasAuthority("ORGANIZER")

                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
```
> **الترتيب مهم:** القواعد الأدق تجي أول. مثلًا `/api/v1/user/me` قبل `/api/v1/user/**`، و `GET /zone/get` (مفتوح) قبل `/zone/**` (منظم).

## 4) أشياء ثانية
- **التسجيل:** `POST /organizer/register` يصير للأدمن بس، و `POST /visitor/register` يحط `role = "VISITOR"` بنفسه. لا تخلون المستخدم يرسل الدور.
- **أول أدمن:** يتضاف من `data.sql`، وكلمة المرور تكون مشفّرة BCrypt.
- **id المنظم:** في `expo-event/add/{organizerId}/...` و `simulation/add/{organizerId}/...` الأفضل تاخذونه من المستخدم الداخل (`@AuthenticationPrincipal`) بدل الرابط. الفرونت حاليًا يرسل id المستخدم الداخل.
- **CORS:** إذا الفرونت داخل `static` ما تحتاجونه. إذا على بورت ثاني، فعّلوا CORS وخلّوه يسمح بـ header اسمه `Authorization`.

## جدول الصلاحيات
| الإجراء | زائر | منظم | أدمن | بدون دخول |
|---|:-:|:-:|:-:|:-:|
| عرض المناطق والفعاليات والروابط | ✓ | ✓ | ✓ | ✓ |
| تسجيل حساب زائر | — | — | — | ✓ |
| تعديل/حذف حسابه | ✓ (نفسه) | — | ✓ | — |
| إدارة المناطق والروابط والفعاليات | — | ✓ | — | — |
| المحاكاة والتدخلات والحضور التاريخي | — | ✓ | — | — |
| إضافة منظم/أدمن، حذف حسابات | — | — | ✓ | — |
