CREATE TABLE `dim_observer` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `observedBy` varchar(255) NOT NULL,
  `observationOf` varchar(255) NOT NULL,
  `type` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `unique_observer` (`observedBy` ASC, `observationOf` ASC)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8;