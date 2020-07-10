# Solactive Test

This is a simple application that holds statistics for instruments, for a sliding window of 60 seconds. It exposes three end-points, one for submitting ticks, one for getting the global stats, and one for instrument-specific stats.


## Running

This is a Spring Boot project, which includes the spring-boot-maven plugin. To launch use:

```
mvn spring-boot:run
```


## Discussion

Since statistics for instruments are independent, an instance of InstrumentStatsCalculator class is used to hold statistics for **each** instrument. Ticks can come in parallel, so this allows for easier synchronization per instrument. When a tick comes in via the /ticks end-point, it is sent to the StatsManager singleton component, which in turn holds a map of all instruments and their respective InstrumentStatsCalculator instance.

The InstrumentStatsCalculator validates that the incoming tick is not older than 60 seconds, and stores it in a doubly linked list, ordered by tick timestamps. Most of the time ticks should come in order, making the addition an O(1) operation, however the worst-case scenario is that this call will run in O(n). Synchronization happens on a method level.

Every time a tick is added the instrument stats are fully recalculated, meaning that unless recalculation is improved (as discussed later) this will always run in O(n).

The StatsManager will also recalculate global stats whenever a new tick is added, as well as when global stats are requested. This is obviously redundant for now, but it's open for improvements later.


## Improvements

I haven't ran any performance metrics on this, however if performance needs to be improved then there's two ways I assume this can happen.

Firstly, ticks come in through the /ticks end-point in JSON, and are converted to a DTO object by the framework which uses jackson. When we pass this DTO down to the StatsManager it creates a new domain object called InstrumentTick, which basically holds everything the tick DTO does except for the instrument name. This is good software engineering practice in terms of design, however it does mean there's double the objects being created for the garbage collector to work on. If there is heavy load posting new ticks, then all these DTO objects needed to be freed. If performance is an issue we could sacrifice "better practices" and use the DTOs in the InstrumentStatsCalculator lists.

Secondly, instrument stats can be "diff" calculated by getting back to the total (avg * count), subtracting from it the elements removed and adding the new element, then getting back to avg by diving by the new count. Similarly if the element added has bigger max or lower min this can be stored. Strictly speaking, full traversal of the list is only needed if one of the elements removed was the min or the max.

Something along these lines can be done for the global stats, however they are a little bit more complicated. If we are not running scheduled jobs to clean old ticks and recalculate, and we want a true sliding widow down to the milisecond, then the result cannot be cached, thus the operation cannot be O(1). The problem arises because you never know when one of the instruments has a tick that expired, so at the very least you need to loop through all the instruments to get the timestamp of the head, and check it against a value stored last global stats recalculation. However, this does not guarantee that the operation will be O(1). As a matter of fact, assuming a constant flow of ticks based on real market data then it probably will almost always be an O(n).

Lastly, there's probably better ways to test ticks expiring than having Thread.sleep(60000) in tests.
