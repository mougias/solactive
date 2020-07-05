# Solactive Test

This is a simple application that holds statistics for instruments, for a sliding window of 60 seconds. It exposes three end-points, one for submitting ticks, one for getting the global stats, and one for instrument-specific stats.


## Running

This is a Spring Boot project, which includes the spring-boot-maven plugin. To launch use:

```
mvn spring-boot:run
```


## Discussion

Since statistics for instruments are independent, an instance of InstrumentStatsCalculator class is used to hold statistics for **each** instrument. Ticks can come in parallel, so this allows for easier synchronization per instrument. When a tick comes in via the /ticks end-point, it is sent to the StatsManager singleton component, which in turn holds a map of all instruments and their respective InstrumentStatsCalculator instance.

The InstrumentStatsCalculator validates that the incoming tick is not older than 60 seconds, and stores it in a LinkedList. Synchronization happens on a method level, so the choice was between LinkedList and the perhaps more commonly used ArrayList. However, assuming that adding ticks is an operation that happens far more often than list traversal, and that no direct access to an ith element is ever needed, LinkedList seemed like a better choice.

The StatsManager runs a scheduled job once a second, that loops through all instruments, and runs cleanAndCalculateStats on them. This method removes ticks older than 60 seconds from the ticks list (something like GC), and calculates count, average, min, max for the remaining. This means that the sliding window is not continuously moving, but slides once per second. This is necessary for caching the results and ensuring that GET stats end-points run in O(1) time.

Traversal of the entire list when cleaning it up is needed, because the order of ticks coming in is not guaranteed to be in accordance with the tick timestamps. Initially I thought of ordering on insert, however that would make insert an O(n) instead of an O(1), for the added benefit that cleaning the list would not require going through all the elements. However, since we are assuming that the number of inserts will be orders of magnitute greater than calculations, keeping the ticks unordered is a much better approach.


## Improvements

I haven't ran any performance metrics on this, however if performance needs to be improved then there's two ways I assume this can happen.

Firstly, the StatsManager runs calculations on instruments sequentially, meaning that only 1 CPU is used. For large number of instruments holding large lists of ticks this might be a problem. A better solution would probably be to fire up a number of threads depending on the current running system number of CPUs, and run cleanAndCalculateStats for a number of instruments in parallel.

Secondly, ticks come in through the /ticks end-point in JSON, and are converted to a DTO object by the framework which uses jackson. When we pass this DTO down to the StatsManager it creates a new domain object called InstrumentTick, which basically holds everything the tick DTO does except for the instrument name. This is good software engineering practice in terms of design, however it does mean there's double the objects being created for the garbage collector to work on. If there is heavy load posting new ticks, then all these DTO objects needed to be freed. If performance is an issue we could sacrifice "better practices" and use the DTOs in the InstrumentStatsCalculator lists.