# Timetabling benchmark summary

Seeds per instance family: 10. SA iterations: 4000000. Intervals are 95% percentile-bootstrap CIs of the mean (10000 resamples).

## medium (200 events, 1000 students, 8 rooms, 45 slots)

| solver | feasible | hard (mean, 95% CI) | soft (mean, 95% CI) | soft sd | time ms (median) |
|---|---:|---:|---:|---:|---:|
| random | 0/10 | 268.8 [258.0, 283.4] | 2689.4 [2563.5, 2837.5] | 239.3 | 0.3 |
| greedy-input | 10/10 | 0.0 [0.0, 0.0] | 2120.7 [2048.7, 2187.7] | 119.0 | 11.5 |
| greedy-random | 10/10 | 0.0 [0.0, 0.0] | 2098.9 [1982.5, 2230.2] | 213.4 | 11.7 |
| greedy-largest-degree | 10/10 | 0.0 [0.0, 0.0] | 1381.7 [1359.3, 1398.6] | 34.1 | 12.6 |
| greedy-dsatur | 10/10 | 0.0 [0.0, 0.0] | 1388.6 [1365.6, 1413.6] | 41.1 | 10.7 |
| sa(greedy-dsatur) | 10/10 | 0.0 [0.0, 0.0] | 1180.7 [1149.2, 1223.3] | 64.2 | 31440.4 |

Paired weighted-objective difference sa(greedy-dsatur) - greedy-dsatur: -207.9 [-251.4, -153.7]; sign-test p = 0.0020.

