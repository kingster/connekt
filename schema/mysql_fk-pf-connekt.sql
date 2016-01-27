CREATE DATABASE IF NOT EXISTS `connekt`;

CREATE TABLE IF NOT EXISTS `USER_INFO`  (
  `userId` varchar(100) NOT NULL DEFAULT '',
  `apikey` varchar(100) NOT NULL DEFAULT '',
  `groups` text,
  `lastUpdatedTS` bigint(20) unsigned NOT NULL,
  `updatedBy` varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY (`userId`),
  UNIQUE KEY `apikey` (`apikey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `RESOURCE_PRIV` (
  `userId` varchar(100) NOT NULL DEFAULT '',
  `userType` enum('GLOBAL','GROUP','USER') DEFAULT 'USER',
  `resources` text,
  PRIMARY KEY (`userId`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `STENCIL_STORE` (
  `id` varchar(40) NOT NULL DEFAULT '',
  `engine` enum('VELOCITY','GROOVY') NOT NULL DEFAULT 'VELOCITY',
  `engineFabric` text NOT NULL,
  `createdBy` varchar(45) NOT NULL DEFAULT '',
  `updatedBy` varchar(45) NOT NULL DEFAULT '',
  `version` int(11) NOT NULL,
  `creationTS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `lastUpdatedTS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;