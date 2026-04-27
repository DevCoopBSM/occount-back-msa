package devcoop.occount.warmup

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan

@AutoConfiguration
@ComponentScan(basePackageClasses = [StartupWarmupConfiguration::class])
@EnableConfigurationProperties(StartupWarmupProperties::class)
class StartupWarmupConfiguration
