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

Every time a tick is added the instrument stats are calculated by calculating the new average (old_avg * old_count + tick_price) / (old_count + 1), and checking the new tick price against the stored min and max. Therefore, instrument stats are always cached, and fetching them is an O(1) operation.

The StatsManager holds a hashmap of all instruments and their respective stats calculator and passes incoming ticks to the appropriate calculator. It then does a full recalculation of the global stats and stores it in its local cache, making the fetching of global stats an O(1) operation as well. The StatsManager is also a thread, which continuously loops through all the instrument stats calculators and clears expired ticks, then recalculates global stats. The global stats calculation is an O(n) operation, when n is the number of instruments.


## Improvements

I haven't ran any performance metrics on this, however if performance needs to be improved then there's two ways I assume this can happen.

Firstly, ticks come in through the /ticks end-point in JSON, and are converted to a DTO object by the framework which uses jackson. When we pass this DTO down to the StatsManager it creates a new domain object called InstrumentTick, which basically holds everything the tick DTO does except for the instrument name. This is good software engineering practice in terms of design, however it does mean there's double the objects being created for the garbage collector to work on. If there is heavy load posting new ticks, then all these DTO objects needed to be freed. If performance is an issue we could sacrifice "better practices" and use the DTOs in the InstrumentStatsCalculator lists.

Secondly, there is a scenario where instrument stats need to be fully recalculated, when one of the elements removed was your min or max. This requires a full traversal of the ticks list to get the new values, however, there could be a smarter approach (e.g. holding a second list ordered by tick price).

Something along these lines can be done for the global stats, however, currently every time a new tick is addeed the global stats are fully recalculated using the cached stats of all instruments, in an O(n) operation where n is the number of instruments. This also happens periodically when expired ticks are cleaned from the instrument lists. A smarter approach could also "diff" calculate global stats somehow.

Lastly, there's probably better ways to test ticks expiring than having Thread.sleep(60000) in tests.
