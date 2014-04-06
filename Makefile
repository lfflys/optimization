
JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = Cloud.java Server.java VM.java Request.java Activity.java Optimization.java Main.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
