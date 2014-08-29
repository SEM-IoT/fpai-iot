
CREATE TABLE `dim_date` (
  `date` date NOT NULL,
  `year` int(11) unsigned NOT NULL,
  `quarter` int(1) unsigned NOT NULL,
  `month` int(2) unsigned NOT NULL,
  `week` int(2) unsigned NOT NULL,
  `day_of_year` int(3) unsigned NOT NULL,
  `day_of_month` int(2) unsigned NOT NULL,
  `day_of_week` int(1) unsigned NOT NULL,
  PRIMARY KEY (`date`),
  UNIQUE KEY `date_UNIQUE` (`date`),
  KEY `day_of_week` (`day_of_week`),
  KEY `month` (`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--

DROP PROCEDURE IF EXISTS `populate_dim_date`;

--

CREATE PROCEDURE `populate_dim_date`()
BEGIN
        declare d date default '2013-01-01';

        delete from `fpai_monitoring`.`dim_date`;
        alter table `fpai_monitoring`.`dim_date` auto_increment = 1;

        while('2015-01-01' > d) do
                INSERT INTO `fpai_monitoring`.`dim_date`
                        (`date`, `year`, `quarter`, `month`, `week`, `day_of_year`, `day_of_month`, `day_of_week`)
                VALUES
                        (d, year(d), floor((month(d)-1) / 3 + 1), month(d), week(d, 0), dayofyear(d), dayofmonth(d), dayofweek(d));

                set d = d + interval 1 day;
        end while;
END

--

call populate_dim_date();
