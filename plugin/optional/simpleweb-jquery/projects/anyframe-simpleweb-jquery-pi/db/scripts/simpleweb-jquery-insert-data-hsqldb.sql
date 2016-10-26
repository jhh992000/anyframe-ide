CREATE MEMORY TABLE JQUERY_GENRE(GENRE_ID VARCHAR(5) NOT NULL,NAME VARCHAR(50) NOT NULL,CONSTRAINT PK_JQUERY_GENRE PRIMARY KEY(GENRE_ID));
CREATE MEMORY TABLE JQUERY_MOVIE(MOVIE_ID VARCHAR(8) NOT NULL,GENRE_ID VARCHAR(5) NOT NULL,TITLE VARCHAR(50) NOT NULL,DIRECTOR VARCHAR(50) NOT NULL,ACTORS VARCHAR(100) NOT NULL,RUNTIME NUMERIC(3),RELEASE_DATE DATE,TICKET_PRICE NUMERIC(6,2),SIMPLE_UPLOAD_FILE_PATH VARCHAR(1000),NOW_PLAYING CHAR(1),CONSTRAINT PK_JQUERY_MOVIE PRIMARY KEY(MOVIE_ID));
CREATE MEMORY TABLE JQUERY_IDS(TABLE_NAME VARCHAR(16) NOT NULL PRIMARY KEY,NEXT_ID DECIMAL(30) NOT NULL);

INSERT INTO JQUERY_GENRE VALUES('GR-01','Action');
INSERT INTO JQUERY_GENRE VALUES('GR-02','Adventure');
INSERT INTO JQUERY_GENRE VALUES('GR-03','Animation');
INSERT INTO JQUERY_GENRE VALUES('GR-04','Comedy');
INSERT INTO JQUERY_GENRE VALUES('GR-05','Crime');
INSERT INTO JQUERY_GENRE VALUES('GR-06','Drama');
INSERT INTO JQUERY_GENRE VALUES('GR-07','Fantasy');
INSERT INTO JQUERY_GENRE VALUES('GR-08','Romance');
INSERT INTO JQUERY_GENRE VALUES('GR-09','Sci-Fi');
INSERT INTO JQUERY_GENRE VALUES('GR-10','Thriller');
INSERT INTO JQUERY_MOVIE VALUES('MV-00001','GR-02','Alice in Wonderland','Tim Burton','Johnny Depp',110,'2010-03-04',8000,'/sample/images/posters/aliceinwonderland.jpg','Y');
INSERT INTO JQUERY_MOVIE VALUES('MV-00002','GR-09','Avatar','James Cameron','Sigourney Weaver',100,'2010-12-16',7000,'/sample/images/posters/avatar.jpg','Y');
INSERT INTO JQUERY_MOVIE VALUES('MV-00003','GR-01','Green Zone','Paul Greengrass','Yigal Naor',90,'2010-03-25',7000,'/sample/images/posters/greenzone.jpg','Y');
INSERT INTO JQUERY_MOVIE VALUES('MV-00004','GR-06','Remember Me','Allen Coulter','Caitlyn Rund',115,'2010-03-12',8000,'/sample/images/posters/rememberme.jpg','Y');
INSERT INTO JQUERY_MOVIE VALUES('MV-00005','GR-04','She is Out of My League','Jim Field Smith','Jay Baruchel',118,'2010-03-12',7500,'/sample/images/posters/shesoutof.jpg','Y');
INSERT INTO JQUERY_MOVIE VALUES('MV-00006','GR-05','Shutter Island','Martin Scorsese','Leonardo DiCaprio',95,'2010-03-18',8000,'/sample/images/posters/shutterisland.jpg','Y');
INSERT INTO JQUERY_IDS VALUES('JQUERY_MOVIE',7);

commit;
