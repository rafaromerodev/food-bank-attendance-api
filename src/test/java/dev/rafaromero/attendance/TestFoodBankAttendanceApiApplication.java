package dev.rafaromero.attendance;

import org.springframework.boot.SpringApplication;

public class TestFoodBankAttendanceApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(FoodBankAttendanceApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
