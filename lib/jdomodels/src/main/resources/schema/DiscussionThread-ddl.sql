CREATE TABLE IF NOT EXISTS `DISCUSSION_THREAD` (
  `ID` bigint(20) NOT NULL,
  `FORUM_ID` bigint(20) NOT NULL,
  `TITLE` tinyblob NOT NULL,
  `ETAG` char(36) NOT NULL,
  `CREATED_ON` TIMESTAMP NOT NULL,
  `CREATED_BY` bigint(20) NOT NULL,
  `MODIFIED_ON` TIMESTAMP NOT NULL,
  `MESSAGE_KEY` varchar(100) NOT NULL,
  `IS_EDITED` boolean NOT NULL,
  `IS_DELETED` boolean NOT NULL,
  `IS_PINNED` boolean NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE (`MESSAGE_KEY`),
  CONSTRAINT `DISCUSSION_THREAD_FORUM_ID_FK` FOREIGN KEY (`FORUM_ID`) REFERENCES `FORUM` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `DISCUSSION_THREAD_CREATED_BY_FK` FOREIGN KEY (`CREATED_BY`) REFERENCES `JDOUSERGROUP` (`ID`)
)
