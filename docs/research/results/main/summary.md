# Timetabling benchmark summary

Seeds per instance family: 10. SA iterations: 1000000. Intervals are 95% percentile-bootstrap CIs of the mean (10000 resamples).

## small (100 events, 400 students, 5 rooms, 45 slots)

| solver | feasible | hard (mean, 95% CI) | soft (mean, 95% CI) | soft sd | time ms (median) |
|---|---:|---:|---:|---:|---:|
| random | 0/10 | 85.4 [75.5, 95.8] | 1028.1 [986.0, 1069.2] | 70.5 | 0.2 |
| greedy-input | 10/10 | 0.0 [0.0, 0.0] | 687.0 [661.7, 712.9] | 44.8 | 4.7 |
| greedy-random | 10/10 | 0.0 [0.0, 0.0] | 644.9 [620.8, 670.9] | 42.4 | 4.1 |
| greedy-largest-degree | 10/10 | 0.0 [0.0, 0.0] | 447.0 [432.4, 462.6] | 26.1 | 3.8 |
| greedy-dsatur | 10/10 | 0.0 [0.0, 0.0] | 444.8 [433.6, 457.6] | 20.9 | 3.8 |
| descent(greedy-dsatur) | 10/10 | 0.0 [0.0, 0.0] | 319.0 [313.6, 324.2] | 9.2 | 5158.8 |
| sa(greedy-dsatur) | 10/10 | 0.0 [0.0, 0.0] | 316.3 [310.8, 323.3] | 10.9 | 4845.4 |

Soft-penalty decomposition (means):

| solver | last period | >2 consecutive | single-class days |
|---|---:|---:|---:|
| random | 153.1 | 3.2 | 871.8 |
| greedy-input | 142.4 | 8.7 | 535.9 |
| greedy-random | 112.5 | 7.1 | 525.3 |
| greedy-largest-degree | 78.9 | 12.9 | 355.2 |
| greedy-dsatur | 81.7 | 13.7 | 349.4 |
| descent(greedy-dsatur) | 3.8 | 7.3 | 307.9 |
| sa(greedy-dsatur) | 3.0 | 5.9 | 307.4 |

Paired weighted-objective differences (negative = first solver better; 95% bootstrap CI; exact two-sided sign test):

| comparison | mean diff | 95% CI | wins/losses/ties | sign-test p |
|---|---:|---:|---:|---:|
| sa(greedy-dsatur) - greedy-dsatur | -128.5 | [-138.3, -120.3] | 10/0/0 | 0.0020 |
| descent(greedy-dsatur) - greedy-dsatur | -125.8 | [-135.4, -116.9] | 10/0/0 | 0.0020 |
| sa(greedy-dsatur) - descent(greedy-dsatur) | -2.7 | [-8.6, 3.9] | 7/3/0 | 0.3438 |
| greedy-dsatur - greedy-largest-degree | -2.2 | [-13.7, 8.3] | 6/4/0 | 0.7539 |
| greedy-largest-degree - greedy-input | -240.0 | [-267.0, -212.0] | 10/0/0 | 0.0020 |

## medium (200 events, 1000 students, 8 rooms, 45 slots)

| solver | feasible | hard (mean, 95% CI) | soft (mean, 95% CI) | soft sd | time ms (median) |
|---|---:|---:|---:|---:|---:|
| random | 0/10 | 268.8 [258.0, 283.4] | 2689.4 [2563.5, 2837.5] | 239.3 | 0.3 |
| greedy-input | 10/10 | 0.0 [0.0, 0.0] | 2120.7 [2048.7, 2187.7] | 119.0 | 15.4 |
| greedy-random | 10/10 | 0.0 [0.0, 0.0] | 2098.9 [1982.5, 2230.2] | 213.4 | 12.1 |
| greedy-largest-degree | 10/10 | 0.0 [0.0, 0.0] | 1381.7 [1359.3, 1398.6] | 34.1 | 11.9 |
| greedy-dsatur | 10/10 | 0.0 [0.0, 0.0] | 1388.6 [1365.6, 1413.6] | 41.1 | 11.9 |
| descent(greedy-dsatur) | 10/10 | 0.0 [0.0, 0.0] | 1179.5 [1169.7, 1190.0] | 17.4 | 8175.0 |
| sa(greedy-dsatur) | 10/10 | 0.0 [0.0, 0.0] | 1238.8 [1211.9, 1267.6] | 46.8 | 7753.7 |

Soft-penalty decomposition (means):

| solver | last period | >2 consecutive | single-class days |
|---|---:|---:|---:|
| random | 519.4 | 22.3 | 2147.7 |
| greedy-input | 378.6 | 23.0 | 1719.1 |
| greedy-random | 395.7 | 22.9 | 1680.3 |
| greedy-largest-degree | 252.4 | 63.0 | 1066.3 |
| greedy-dsatur | 253.7 | 63.9 | 1071.0 |
| descent(greedy-dsatur) | 3.4 | 46.6 | 1129.5 |
| sa(greedy-dsatur) | 28.2 | 29.5 | 1181.1 |

Paired weighted-objective differences (negative = first solver better; 95% bootstrap CI; exact two-sided sign test):

| comparison | mean diff | 95% CI | wins/losses/ties | sign-test p |
|---|---:|---:|---:|---:|
| sa(greedy-dsatur) - greedy-dsatur | -149.8 | [-180.7, -114.5] | 10/0/0 | 0.0020 |
| descent(greedy-dsatur) - greedy-dsatur | -209.1 | [-228.1, -189.8] | 10/0/0 | 0.0020 |
| sa(greedy-dsatur) - descent(greedy-dsatur) | 59.3 | [32.2, 87.6] | 1/9/0 | 0.0215 |
| greedy-dsatur - greedy-largest-degree | 6.9 | [-11.9, 26.4] | 5/5/0 | 1.0000 |
| greedy-largest-degree - greedy-input | -739.0 | [-821.6, -654.5] | 10/0/0 | 0.0020 |

## large (400 events, 2000 students, 12 rooms, 45 slots)

| solver | feasible | hard (mean, 95% CI) | soft (mean, 95% CI) | soft sd | time ms (median) |
|---|---:|---:|---:|---:|---:|
| random | 0/10 | 605.4 [572.8, 644.0] | 5309.1 [5112.1, 5540.3] | 367.6 | 0.6 |
| greedy-input | 1/10 | 5.7 [3.0, 8.8] | 4886.9 [4770.1, 5023.1] | 215.9 | 23.7 |
| greedy-random | 0/10 | 8.0 [4.5, 12.2] | 4952.6 [4790.1, 5139.3] | 301.6 | 24.1 |
| greedy-largest-degree | 10/10 | 0.0 [0.0, 0.0] | 3262.1 [3224.9, 3296.2] | 61.0 | 23.8 |
| greedy-dsatur | 10/10 | 0.0 [0.0, 0.0] | 3270.8 [3236.1, 3302.7] | 56.9 | 24.6 |
| descent(greedy-dsatur) | 10/10 | 0.0 [0.0, 0.0] | 3006.0 [2988.2, 3023.5] | 30.3 | 8017.1 |
| sa(greedy-dsatur) | 10/10 | 0.0 [0.0, 0.0] | 3114.9 [3075.0, 3157.3] | 69.6 | 7883.8 |

Soft-penalty decomposition (means):

| solver | last period | >2 consecutive | single-class days |
|---|---:|---:|---:|
| random | 949.2 | 42.5 | 4317.4 |
| greedy-input | 881.2 | 32.9 | 3972.8 |
| greedy-random | 926.0 | 31.8 | 3994.8 |
| greedy-largest-degree | 368.6 | 80.1 | 2813.4 |
| greedy-dsatur | 376.8 | 81.4 | 2812.6 |
| descent(greedy-dsatur) | 52.3 | 64.2 | 2889.5 |
| sa(greedy-dsatur) | 97.2 | 50.9 | 2966.8 |

Paired weighted-objective differences (negative = first solver better; 95% bootstrap CI; exact two-sided sign test):

| comparison | mean diff | 95% CI | wins/losses/ties | sign-test p |
|---|---:|---:|---:|---:|
| sa(greedy-dsatur) - greedy-dsatur | -155.9 | [-189.2, -119.6] | 10/0/0 | 0.0020 |
| descent(greedy-dsatur) - greedy-dsatur | -264.8 | [-286.3, -243.5] | 10/0/0 | 0.0020 |
| sa(greedy-dsatur) - descent(greedy-dsatur) | 108.9 | [75.1, 144.3] | 0/10/0 | 0.0020 |
| greedy-dsatur - greedy-largest-degree | 8.7 | [-30.2, 47.9] | 5/5/0 | 1.0000 |
| greedy-largest-degree - greedy-input | -5701624.8 | [-8801543.5, -3001594.2] | 10/0/0 | 0.0020 |

