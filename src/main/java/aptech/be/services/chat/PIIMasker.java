package aptech.be.services.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PIIMasker {
	private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
	private static final Pattern PHONE_PATTERN = Pattern.compile("(?:(?:\\+?84|0)\\d{9,10})");
	private static final Pattern HOUSE_NUMBER_PATTERN = Pattern.compile("\\b\\d+\\s+");

	public static String mask(String input) {
		if (input == null || input.isBlank()) return input;

		String masked = replaceAll(input, EMAIL_PATTERN, m -> maskEmail(m.group()));
		masked = replaceAll(masked, PHONE_PATTERN, m -> maskPhone(m.group()));
		masked = HOUSE_NUMBER_PATTERN.matcher(masked).replaceAll("");
		return masked;
	}

	private static String replaceAll(String input, Pattern pattern, java.util.function.Function<Matcher, String> replacer) {
		Matcher matcher = pattern.matcher(input);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String replacement = replacer.apply(matcher);
			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private static String maskEmail(String email) {
		int at = email.indexOf('@');
		if (at <= 1) return "***@" + email.substring(at + 1);
		String name = email.substring(0, at);
		String domain = email.substring(at + 1);
		return name.charAt(0) + "***@" + domain;
	}

	private static String maskPhone(String phone) {
		if (phone.length() <= 4) return "****";
		return "*******" + phone.substring(phone.length() - 3);
	}
} 