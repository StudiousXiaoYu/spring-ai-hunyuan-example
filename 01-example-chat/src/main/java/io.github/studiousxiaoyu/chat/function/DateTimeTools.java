package io.github.studiousxiaoyu.chat.function;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;

@Slf4j
public class DateTimeTools {

    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrentDateTime() {
        String currentDateTime = LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
        log.info("getCurrentDateTime:{}",currentDateTime);
        return currentDateTime;
    }

}