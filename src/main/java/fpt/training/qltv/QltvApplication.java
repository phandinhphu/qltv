package fpt.training.qltv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QltvApplication {

	public static void main(String[] args) {
		SpringApplication.run(QltvApplication.class, args);
	}

}
