# StravaActivitiesUpdater
This is a Java program that uses Strava v3 API to change your bike activities automatically.

The program will change :

* the name / description of the activity
* the bike you used
* it will make the activity private (so you won't spam your friends with work rides)  
(This behaviour can be turned off)
* It will flag the activity as Commute

Use this program to update your commute rides automatically !

## Motivation

I like Strava for keeping track of my mileage. I use it a lot, and I like having wear information on my bikes that way.

I have two bikes : a *road bike* I use for competition / training, and a **work bike** I use twice a day.

I used to only record my *road bike* rides, because if I was to record **work rides**, the mileage would count on my default bike, the *road one*.  
If I did record my **work rides**, I would have to load Strava every day to change the bike used during my commutes manually... Every day...

So this is why this program is here !  
It makes my commutes use the **work bike**, and it makes these rides privates, so my friends are not spammed with me riding half an hour twice a day.

In my Strava profile, I've set my *road bike* set as default. These road activities won't be updated by this program, because either they are longer than my work commutes, or they are done on week-ends.

## How it works

The program uses both Strava v3 API and Strava's website to update your activities.

The activities that will be updated will be :
* Activities performed from monday to friday. So no week-end rides will ever get updated.
* Activities that lasted less than **`app.max_time_to_update`**.
* Activities that are not already flagged by Strava as commutes.

The program will then, for each matching activity :
* Through the use of the API, change the activity's name, description, used bike, and commute-flag.
* Though the website, turn your activity private.


The API requests use the parameters **`app.client_id`**, **`app.client_secret`** and **`user.id`**

The website connection uses the parameters **`user.name`** and **`user.password`**


### Limitations

There are limitations to this program :

* It will not update activities already flagged as commutes.
* It uses a framework to surf on Strava's website, to make activities private; because there is no way to change the privacy cleanly with the v3 API...  
If the website is updated at some point, this privacy-making could cease to work.  
The program is done in such a way that failing to turn an activity private won't crash everything.
* The program will only scan up to your last 50 activities, so be sure to run it every once in a while.


## Running the program

Follow these steps to get the program running.  
For now, the program is a single launch application that dies upon finish.  
It is intended to be run periodically. A CRON or something like that should do the job.

### Prerequisites

You need Java 1.8 installed.

### Steps

Download the latest [release](https://github.com/Spriggans12/StravaActivitiesUpdater/releases/latest) of the program.

Extract the jar file, and put the `constants.properties` file right next to it.  
Then, you need to edit the `constants.properties` file (read section below).

After that, you simply execute the program with :
```
java -jar strava-update-gears.jar
```

### Program parameters

You need to set every parameter present in the `constants.properties` file.  
Here's a list of every parameter, what it does, and - sometimes - additionnal explanation.  
The parameters must be set in a `parameter=value` fashion. No need to use space characters.

- **`app.client_id`**  
This is your personnal Strava application ID  
Go to your [Strava API page](https://www.strava.com/settings/api)  
Read your application ID there. You may need to create an application if you haven't yet.

- **`app.client_secret`**  
This is your personnal Strava application secret  
You can find it the same way as **`app.client_id`** 

- **`app.code`**  
This is where it gets tricky.  
This encrypted code means that you have allowed the Strava application depicted by **`app.client_id`** to make changes to your Strava account.  
To get this code, you must manually perform a OAuth process with Strava, to aquire an **access_token**.  
Here's a way to do so :
  - Open a browser, and go to https://www.strava.com/oauth/authorize?approval_prompt=force&response_type=code&scope=read,read_all,profile:read_all,profile:write,activity:read,activity:read_all,activity:write&redirect_uri=http%3A%2F%2Flocalhost&client_id=<APP_ID>  
  Where <APP_ID> is your **`app.client_id`** you have written earlier
  - Click Agree : it should go to into a *Not Found* page. That's normal.
  - Inspect the URL. You will find a `&code=<MY_CODE>&scope=...` in it.
  - Use the <MY_CODE> value for the **`app.code`** parameter.


- **`app.make_private`**  
Can be either 0 or 1  
If this is set to 1, the program will connect to Strava's website using your credentials.  
If this is set to 0, activities will remain public, and the parameters **`user.name`** and **`user.password`** can be ommited.  
Note that this is hackish, as this is not part of the official API : if Strava's website changes, it may no longer work.

- **`app.update_regardless_of_date`**  
Can be either 0 or 1  
Set this to 0 to only update activities since the last launch of the program.  
Set that to 1 to update all your activities.

- **`app.date_file_path`**  
This is the location of the file containing the last execution date.  
This location should be accessible on your drive. If the file does not exist, it will be created.  
Examples : /home/pi/strava/lastExecution.txt  OR  C:/Users/you/strava/lastExecution.txt

- **`app.max_time_to_update`**  
Time in seconds that the activity's duration should not exceed.  
If the activity is longer than that, the activity won't be updated.  
This makes it so your offday long rides won't be seen as work commutes, as they should exceed this value.

- **`app.ignore_ssl`**  
Can be either 0 or 1  
If set to 1, ignores certificates checks completely, and thus SSL.  
This should **realy** be set to 0  
Only use the 1 value in a testing environment.

- **`app.logs_level`**  
Sets the log level. Exceptions will still be displayed anyways  
Can be one of : ALL, FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE, OFF  
This parameter is case-sensitive

- **`user.id`**  
You can find your Athlese ID in Strava's website, in your profile page.

- **`user.work_bike`**  
Strava ID of the bike you want to use for work commutes.  
How to get this id is easy :
  - Go [there](https://www.strava.com/settings/gear)
  - Click on your work bike
  - Find your gear ID in the URL
  - Append **b** to this gear ID, and use that as a value (eg URL /bikes/123456 will mean you should use `user.work_bike=b123456` in the file)

- **`user.name`**  
Your email for connecting to Strava.  
This parameter is not needed if you choose to set **`app.make_private`** to 0

- **`user.password`**  
Your password for connecting to Strava.  
This parameter is not needed if you choose to set **`app.make_private`** to 0

- **`activity.name.XXX`**  
These parameters allow you to change what your updated activities names will be.  
Different values can be configured if the activity was a morning or an evening ride.  
These values can remain empty.

- **`activity.desc.XXX`**  
These parameters allow you to change what your updated activities descriptions will be.
Different values can be configured if the activity was a morning or an evening ride.  
These values can remain empty.


## Compiling the program yourself

If you somewhy want to change the code, you need to use Maven to make a executable jar file with your changes.  
Go to the root folder, and run :
```
mvn clean package
```
This program was developped using Eclipse.


## License

This project is licensed under the Do What The F*ck You Want To Public License - see the [LICENSE.md](LICENSE.md) file for details, but yeah, it means I don't care who uses this or how.

## Acknowledgments

* Strava, I guess
* Also, [this guy](https://github.com/danshannon/javastravav3api) for making the Java API. Many thanks to him !
