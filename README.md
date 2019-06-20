# StravaActivitiesUpdater
This is a Java program that uses Strava v3 API to change your bike activities automatically.

The changes will include a change of your used bike, and a change in the description / name of the activities.

Use this program to update your commutes rides automatically !

## Motivation

I like Strava for keeping track of my mileage. I use it a lot, and I like having wear information on my bikes that way.

I have two bikes : a road bike I use for competition / training, and a work bike.

I go to work daily using the work bike.

I used to only record my road bike rides, as if I did, the mileage would count on my default bike, the road one.

I don't like loading Strava every day and having to change the gear used during my commutes manually.

So this is why this program is here. It makes my commutes use the work bike, and it make these rides privates, so my friends are not spammed with me riding half an hour twice a day.


## Running the program

Follow these steps to get the program running.
For now, the program is a single launch application that dies upon finish.
It is intended to be run periodically. Like a CRON or something should do the job.

### Prerequisites

You need Java 1.8 installed.

### Steps

Download the latest [release](https://github.com/Spriggans12/StravaActivitiesUpdater/releases/latest) of the program.

Extract the jar file, and put the constants.properties file right next to it.

Then, you need to edit the constants.properties file (see section below for assistance).

After that, you simply execute the program with :
```
java -jar strava-update-gears-x.y.z.jar
```


## Compiling the program yourself

If you somewhy want to change the code, you need to use Maven to make a executable jar file with your changes.
Go to the root folder, and run :
```
mvn clean package
```
This program was developped using Eclipse Neon.


## License

This project is licensed under the Do What The F*ck You Want To Public License - see the [LICENSE.md](LICENSE.md) file for details, but yeah, it means I don't care who uses this or how.

## Acknowledgments

* Strava, I guess
* Also, [this guy](https://github.com/danshannon/javastravav3api) for making the Java API. Many thanks to him !

