
CREATE TABLE `dim_time` (
  `time` time NOT NULL,
  `hour` int(2) unsigned NOT NULL,
  `minute` int(2) unsigned NOT NULL,
  PRIMARY KEY (`time`),
  UNIQUE KEY `time_UNIQUE` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--

DROP PROCEDURE IF EXISTS `populate_dim_time`;

--

CREATE PROCEDURE `populate_dim_time`()
BEGIN
        declare h int default 0;
        declare m int default 0;

        delete from `fpai_monitoring`.`dim_time`;
        alter table `fpai_monitoring`.`dim_time` auto_increment = 1;

        ins: loop
                INSERT INTO `fpai_monitoring`.`dim_time` (`time`, `hour`, `minute`) VALUES (concat(h,":",m,":0"), h, m);

                set m = m + 1;

                if m = 60 then
                        set h = h + 1;
                        set m = 0;
                end if;

                if h = 24 then
                        leave ins;
                end if;
        end loop;
END

--

call populate_dim_time();
