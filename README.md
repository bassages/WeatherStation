WeatherStation
==============

Weather Station for android displaying temperature, humidity and barometric pressure 
which are read by a TI SensorTag.

Features:
- The unit in which the temperature is displayed can be configured to be fahrenheit or celcius
- The user can be notified when the temperature get's higher than a user defined value
- The user can be notified when the temperature get's lower than a user defined value

Tested on:
Samsung Galaxy S4


Currently under development:

 - Setting a temperature alarm.-
 When the temperature exceeds the set min/max pref, a notification is created.

Backlog:
- Maintain history (store only)
- Implement a simple history viewer
  - https://software.intel.com/en-us/android/articles/a-look-at-data-analysis-with-charts-and-graphs-in-android-apps
- Integrate the TI sensortag IR temp sensor  