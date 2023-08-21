CREATE TABLE defenceOfThesis
   (
     institutionID         INTEGER        NOT NULL    -- ID of the higher education institution 
   , courseID              SMALLINT       NOT NULL    -- ID of the course at the institution
   , studentID             CHAR(10)       NOT NULL    -- unique ID of the student
   , dueDate               DATE           NOT NULL    -- due date for submitting the theseis before the defence
   , dateOfDefence         DATE           NOT NULL
   , seq                   SMALLINT       NOT NULL    -- sequence number of the defence of the thesis
   , degree                VARCHAR(20)    NOT NULL    -- the degree (bachelor's, master's...) or title obtained by defending the thesis 
   , grade                 CHAR(2)                    -- ECTS grading system (A-F)
   , note                  VARCHAR(255)

   , CONSTRAINT pkDefenceOfThesis
         PRIMARY KEY (studentID, institutionID, courseID, dueDate, seq) 
   , CONSTRAINT fkDefenceOfThesisCourse
         FOREIGN KEY (courseID, institutionID)   
         REFERENCES course(courseID, institutionID) 
   , CONSTRAINT fkDefenceOfThesisStudent
         FOREIGN KEY (studentID)
         REFERENCES student(studentID)
   );
   
CREATE TABLE student 
   (
     studentID             CHAR(10)       NOT NULL
   , nationalID            CHAR(11)       NOT NULL    -- national identification number ("OIB" in Croatia)
   , lastName              VARCHAR(40)    NOT NULL 
   , firstName             VARCHAR(40)    NOT NULL 
   , email                 VARCHAR(100)   NOT NULL
   , sex                   CHAR(1)
   , dateOfBirth           DATE 
   , placeOfBirth          VARCHAR(100)
   , placeOfResidence      VARCHAR(100)
   , nationality           VARCHAR(20)
   , institutionID         INTEGER        NOT NULL    -- the institution the student is enrolled in 
   
   , CONSTRAINT pkStudent
         PRIMARY KEY (studentID)
   , CONSTRAINT fkStudentInstitution
         FOREIGN KEY (institutionID)
         REFERENCES institution(institutionID)
   );
   
CREATE TABLE course 
   (
     courseID              SMALLINT       NOT NULL
   , institutionID         INTEGER        NOT NULL
   , courseName            VARCHAR(100)   NOT NULL
   , levelOfStudy          VARCHAR(20)    NOT NULL    -- undergraduate, graduate etc.
   
   , CONSTRAINT pkCourse
         PRIMARY KEY (courseID, institutionID)
   , CONSTRAINT fkCourseInstitution
         FOREIGN KEY (institutionID)
         REFERENCES institution(institutionID)
   );
   
CREATE TABLE institution
   (
     institutionID         INTEGER        NOT NULL    AUTO_INCREMENT
   , abbreviation          VARCHAR(10)                -- abbreviation of the name
   , institutionName       VARCHAR(200)   NOT NULL 
   , parentInstitutionID   INTEGER
   , email                 VARCHAR(100)
   , country               VARCHAR(50)
   , city                  VARCHAR(50)
   
   , CONSTRAINT pkInstitution
         PRIMARY KEY (institutionID)
   , CONSTRAINT fkInstitutionInstitution
         FOREIGN KEY (parentInstitutionID)
         REFERENCES institution(institutionID)
   );