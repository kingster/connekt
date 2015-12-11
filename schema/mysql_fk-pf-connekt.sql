CREATE DATABASE `fk-pf-connekt`;

CREATE TABLE `USER_INFO` (
  `userId` varchar(100) NOT NULL DEFAULT '',
  `apikey` varchar(100) NOT NULL DEFAULT '',
  `groups` text,
  `lastUpdatedTs` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updatedBy` varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RESOURCE_PRIV` (
  `userId` varchar(100) NOT NULL DEFAULT '',
  `userType` enum('GLOBAL','GROUP','USER') DEFAULT 'USER',
  `resources` text,
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
