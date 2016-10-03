
CREATE  TABLE IF NOT EXISTS `users` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT ,
  `createDate` DATETIME NOT NULL ,
  `updateDate` DATETIME NOT NULL ,
  `fbId` VARCHAR(256) ,
  `slackId` VARCHAR(256) ,
  `slackTeamId` VARCHAR(256) ,
  `slackChannel` VARCHAR(256) ,
  `slackToken` VARCHAR(256) ,
  `email` VARCHAR(256) ,
  `first_name` VARCHAR(128) ,
  `last_name` VARCHAR(128) ,
  `profile_pic` VARCHAR(512) ,
  `locale` VARCHAR(16) ,
  `timezone` INT DEFAULT 0 ,
  `gender` VARCHAR(32) ,
  `faves` VARCHAR(512) ,
  `stopped` INT DEFAULT 0 ,
  PRIMARY KEY (`id`) )
ENGINE = InnoDB
AUTO_INCREMENT = 1
DEFAULT CHARACTER SET = utf8;

CREATE  TABLE IF NOT EXISTS `tokens` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT ,
  `updateDate` DATETIME NOT NULL ,
  `teamId` VARCHAR(256) ,
  `botToken` VARCHAR(256) ,
  PRIMARY KEY (`id`) )
ENGINE = InnoDB
AUTO_INCREMENT = 1
DEFAULT CHARACTER SET = utf8;

CREATE  TABLE IF NOT EXISTS `newsitems` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT ,
  `saveDate` DATETIME NOT NULL ,
  `updateDate` DATETIME NOT NULL ,
  `topic` VARCHAR(64) ,
  `title` VARCHAR(256) ,
  `subtitle` VARCHAR(4096) ,
  `article` LONGTEXT ,
  `imageUrl` VARCHAR(512) ,
  `articleUrl` VARCHAR(512) ,
  PRIMARY KEY (`id`) )
ENGINE = InnoDB
AUTO_INCREMENT = 1
DEFAULT CHARACTER SET = utf8;

