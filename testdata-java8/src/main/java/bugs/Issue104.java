package bugs;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Issue104 {
	public static void main(String[] args) {
		System.out.println(run());
	}

	public static String run() {
		LocalDateTime time = LocalDateTime.now();
		ZonedDateTime zdt = time.atZone(ZoneId.systemDefault());
		return zdt.toString();
	}
	
}
