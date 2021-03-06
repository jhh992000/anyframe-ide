drop table BOARD_USER;
drop table BOARD;
drop table BOARD_MASTER;

CREATE TABLE BOARD_USER(
  USER_ID VARCHAR2(20) NOT NULL,
  USER_NAME VARCHAR2(50) NOT NULL,
  PASSWORD VARCHAR2(10) NOT NULL,
  AGE NUMBER(3),
  CELL_PHONE VARCHAR2(14),
  ADDR VARCHAR2(100),
  EMAIL VARCHAR2(50),
  REG_DATE DATE,
  AUTHORITY VARCHAR2(50) NOT NULL,
  CONSTRAINT PK_BOARD_USER PRIMARY KEY (USER_ID) 
);

CREATE TABLE BOARD_MASTER(
BOARD_MASTER_ID NUMBER(10),
TITLE VARCHAR2(100) default '' NOT NULL,
DISPLAY_ORDER NUMBER default 0 NOT NULL,
MODERATED NUMBER default 0,
CONSTRAINT PK_BOARD_MASTER PRIMARY KEY (BOARD_MASTER_ID) 
);


CREATE TABLE BOARD(
BOARD_ID NUMBER,
BOARD_MASTER_ID NUMBER(10) NOT NULL,
BOARD_NAME VARCHAR2(150) default '' NOT NULL,
BOARD_DESC VARCHAR2(255) default NULL,
BOARD_ORDER NUMBER default 1,
BOARD_TOPICS NUMBER default 0 NOT NULL,
REG_DATE DATE,
CONSTRAINT PK_BOARD PRIMARY KEY (BOARD_ID,BOARD_MASTER_ID), 
CONSTRAINT FK_BOARD FOREIGN KEY(BOARD_MASTER_ID) REFERENCES BOARD_MASTER(BOARD_MASTER_ID)
);
