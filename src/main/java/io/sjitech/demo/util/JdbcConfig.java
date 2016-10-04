package io.sjitech.demo.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by wang on 2016/07/15.
 */
@Component
@ConfigurationProperties(prefix="jdbc")
public class JdbcConfig {
}
