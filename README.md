# Java-Network-Capturer
Capturing and analyzing network packages using Jpcap library.
For BUAA CST Java Programming Course Design.

# Usage
1. Put `Jpcap.dll` in your `default_jre(>1.8.0)/bin` folder.
2. Start program.
3. Search device, the Current Device label should show something.
4. Start capture, until the Stop button is clicked.
5. File->Save to, save file in your preferred path.
# Preview
![qq 20181111221147](https://user-images.githubusercontent.com/16256802/48314254-eb30cd80-e601-11e8-98f2-08ce93c3c1af.png)  
![qq 20181111221442](https://user-images.githubusercontent.com/16256802/48314272-24693d80-e602-11e8-8230-9e54b80e390e.png)
# For developers
- Eclipse:
    1. Configure Build Path, add `jpcap.jar` to your Libraries.
    2. Change Native library location to the dictionary of `Jpcap.dll`.
- VS Code:
    1. Put `Jpcap.dll` in your `default_jre(>1.8.0)/bin` folder.
    2. Edit the `.classpath` file and configure the library path.
