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

CREATE TABLE `DATA_STORE` (
  `key` varchar(45) NOT NULL DEFAULT '',
  `type` varchar(32) NOT NULL,
  `value` blob NOT NULL,
  `creationTS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `lastUpdatedTS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SUBSCRIPTIONS` (
  `id` varchar(100) NOT NULL DEFAULT '',
  `name` varchar(100) NOT NULL DEFAULT '',
  `sink` varchar(5000) NOT NULL DEFAULT '',
  `createdBy` varchar(100) NOT NULL,
  `createdTS` date DEFAULT NULL,
  `lastUpdatedTS` date DEFAULT NULL,
  `stencilId` varchar(100) DEFAULT NULL,
  `shutdownThreshold` int(11) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
