package com.autumn.test;

import com.autumn.annotation.ComponentScan;
import com.autumn.annotation.Import;
import com.autumn.test.imported.LocalDateConfiguration;
import com.autumn.test.imported.ZonedDateConfiguration;

@ComponentScan
@Import({ LocalDateConfiguration.class, ZonedDateConfiguration.class })
public class ScanApplication {

}