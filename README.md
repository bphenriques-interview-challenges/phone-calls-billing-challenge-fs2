[![Test](https://github.com/bphenriques-interview-challenges/phone-calls-billing-challenge-fs2/actions/workflows/test.yaml/badge.svg)](https://github.com/bphenriques-interview-challenges/phone-calls-billing-challenge-fs2/actions/workflows/test.yaml)

Cats/Cats-Effects/FS2 counterpart of another challenge I made in 2017 using plain-scala:
- Using a mixture of `fs2-io` and `fs2-data` to process CSV files.
- Using `Bill.Monoid` to combine multiple `Bill`.
- Use `fs2.Stream[IO, CallRecord]` to process records using a streaming approach.

Moreover, made some relevant changes:
- Removed several Scala Docs. The code is self-explanatory (as it should be).
- Severely simplified the abstractions used. It was a bit too complex.
- Simplified the domain model to avoid some verifications.
- Sample Github action workflow to run the tests.
- Simplified project dependencies given how small the project is.
- Simplified documentation.
---

# Goal

Given a list of calls with the following format:

    time_of_start;time_of_finish;call_from;call_to

And the following rules:

 - The first 5 minutes of each call are billed at 5 cents per minute
 - The remainder of the call is billed at 2 cents per minute
 - The caller with the highest total call duration of the day will not be charged (i.e., the caller that has the highest total call duration among all of its calls)

Calculate the total cost for these calls.

## Additional Notes

* A call between `23:59:00` and `01:00:00` means that the call lasted 2 minutes.
* Both `time_of_start` and `time_of_finish` are in the same timezone.
* The following phone numbers are considered different: `+351911234567`, `00351911234567`, and `911234567`.

# Example

Input:
```
09:11:30;09:15:22;+351914374373;+351215355312
15:20:04;15:23:49;+351217538222;+351214434422
16:43:02;16:50:20;+351217235554;+351329932233
17:44:04;17:49:30;+351914374373;+351963433432
```

Output:
``` 
0.51
``` 

## Running

See [Quick Guide](CONTRIBUTING.md#quick-guide) on how to install the application, then:
```bash
$ bin/billing-fs2 <path-to-file>
```

Where `<path-to-file>` is the path to the file.
