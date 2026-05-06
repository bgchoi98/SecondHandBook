package project.config;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.*;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Properties;

@Configuration
@MapperScan(basePackages = { "project" }, annotationClass = Mapper.class)
@ComponentScan(basePackages = { "project" })
@EnableWebMvc
@EnableTransactionManagement
@PropertySource(value = {
        "classpath:application.properties",
        "file:/srv/secondhandbook/config/application.properties"
}, ignoreResourceNotFound = true)
public class MvcConfig implements WebMvcConfigurer {

    @Value("${db.driver}")
    private String driver;
    @Value("${db.url}")
    private String url;
    @Value("${db.username}")
    private String username;
    @Value("${db.password}")
    private String password;

    // ================= Mail =================
    @Value("${mail.username}")
    private String mailUsername;
    @Value("${mail.password}")
    private String mailPassword;

    // ================= Redis =================
    @Value("${redis.host}")
    private String redisHost;
    @Value("${redis.port}")
    private int redisPort;
    @Value("${file.dir}")
    private String uploadPath;

    // JSP ViewResolver
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.jsp("/WEB-INF/views/", ".jsp");
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    // ================= Property =================
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreResourceNotFound(true);
        return configurer;
    }

    // 이미지 경로 매핑 (임시 S3사용시 필요없음)
    // @Override
    // public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // registry.addResourceHandler("/img/**").
    // addResourceLocations("file:" + uploadPath + "/");
    // }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/img/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }

    // hikaricp
    @Bean
    @Primary
    public HikariDataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 공용 RDS를 위한 커넥션 풀 설정
        dataSource.setMaximumPoolSize(5); // 최대 커넥션 수 (공용이므로 작게)
        dataSource.setMinimumIdle(2); // 최소 유휴 커넥션
        dataSource.setConnectionTimeout(30000); // 커넥션 획득 대기 시간 (30초)
        dataSource.setIdleTimeout(600000); // 유휴 커넥션 유지 시간 (10분)
        dataSource.setMaxLifetime(1800000); // 커넥션 최대 수명 (30분)

        return dataSource;
    }

    // mybatis
    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean ssf = new SqlSessionFactoryBean();
        ssf.setDataSource(dataSource());

        // Java Config로 모든 MyBatis 설정
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();

        config.setMapUnderscoreToCamelCase(false);
        config.setJdbcTypeForNull(org.apache.ibatis.type.JdbcType.NULL);
        config.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);

        ssf.setConfiguration(config);

        org.springframework.core.io.support.PathMatchingResourcePatternResolver resolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
        ssf.setMapperLocations(resolver.getResources("classpath:**/*Mapper.xml"));

        return ssf.getObject();
    }

    // ================= TransactionManager =================
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    // ================= MultipartResolver =================
    @Bean
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setMaxUploadSize(5 * 1024 * 1024);
        resolver.setDefaultEncoding("utf-8");
        return resolver;
    }

    @Bean
    public JavaMailSenderImpl mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        return mailSender;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    public StringRedisTemplate redisTemplate() {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }

}
