Repository for tests concerning the includeSource attribute in target files.

It contains 2 bundle IUs and 2 source bundle IUs:
  simple.bundle
  simple.bundle.source
  nosource.bundle
  unrelated.bundle.source

The test scenarios so far are:
1. Target file in slicer mode selects simple.bundle and has includeSource=true
   Assume that simple.bundle and simple.bundle.source are present in the resolved target platform.
