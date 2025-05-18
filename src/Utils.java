import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class Utils {
    public static DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS ");
    public static String decode(String pass) {
        try {
            String key = "salt!1234!@#$<>%";
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(pass));
            return new String(decryptedBytes);
        } catch (Exception e) {
            //throw new RuntimeException(e);
        }
        return "";
    }
    public static String encode(String pass) {
        String encryptedText = "";
        try {
            String key = "salt!1234!@#$<>%";
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(pass.getBytes());
            encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedText;
    }
    public static String unSlashString(String value,String src,  String tgt) {
        return value.replace(src,tgt);
    }

    public static String unQuotaString(String value) {
        return value.substring(1,value.length() - 1);
    }

    public static void logWithTime(String str) {
        System.out.println("--> " + LocalDateTime.now().format(fmt) + str);
    }
    public static void log(String str) {
        System.out.println(str);
    }
    public static String formatCmd(String cmd) {
        return "CMD: " + cmd;
    }

    public static String formatSQL(String sql) {
        return "SQL: " + sql;
    }
    public static String getElapsedTime(Instant instant1, Instant instant2) {
        Duration duration = Duration.between(instant1, instant2);
        StringBuilder sb = new StringBuilder();
        //long d =  duration.toDaysPart();
        long d = duration.toDays();
        if(d > 0) {
            sb.append(d).append(" days ");
            duration = duration.minusDays(d);
        }
        //d = duration.toHoursPart();
        d = duration.toHours();
        if(d > 0) {
            sb.append(d).append(" hours ");
            duration = duration.minusHours(d);
        }
        //d = duration.toMinutesPart();
        d = duration.toMinutes();
        if(d > 0) {
            sb.append(d).append(" minutes ");
            duration = duration.minusMinutes(d);
        }
        sb.append(duration.toMillis() / 1000.0).append(" seconds");
        return "elapsed: " + sb;
    }
}
