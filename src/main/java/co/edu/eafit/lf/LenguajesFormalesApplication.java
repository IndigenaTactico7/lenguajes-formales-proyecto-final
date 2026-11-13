package co.edu.eafit.lf;

import java.util.Arrays;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class LenguajesFormalesApplication {

    private static final String WEB_OPTION = "--web";

    public static void main(String[] args) {
        boolean web = Arrays.asList(args).contains(WEB_OPTION);
        SpringApplicationBuilder app = new SpringApplicationBuilder(LenguajesFormalesApplication.class)
                .web(web ? WebApplicationType.SERVLET : WebApplicationType.NONE);
        if (!web) {

            app.properties("logging.level.root=OFF", "spring.main.log-startup-info=false");
        }
        app.run(args);
    }
}
