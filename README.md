INSTALL

clone the repository

build the program with\
$ gradle build

rename obs_layout.json.example to obs_layout.json, you'll need to change the file paths in there later.\
that's in the main tutorial.\
if new things have been added to the config you'll need to update the file manually.\
to use ranked live match api instead of rsa, rename liveapi_config.json.example to liveapi_config.json and input username and privatekey. set enabled to true.\

make sure obs websocket is enabled with port 4455 and authentication disabled.\
create a symbolic link to spectate_match.json in the folder where you run the program. it must be named spectate_match.json as well.\
$ ln -s path/to/spectate_match.json spectate_match.json

there's a different command for this on windows you can look that up.

now you can run the program with\
$ java -jar app/build/libs/app.jar

how to use: usage.txt\
command list: commands.txt

PERCH TIMER

the perch timer shows how long until each player's dragon perches, in the corner of their pov. it shows up once the dragon is past the zero node and goes away when the dragon starts to perch. the small green and red numbers are how much sooner or later it could be.\
it reads the replay files the rsa client writes, so it only works when the program runs on the same computer as the rsa game. the game folder is found automatically, you just need minecraft open.

put the private room presets zip for the league in the folder where you run the program. it must be named presets.zip.\
the seed number picks the preset, so seed 1 uses the preset named #1 and so on. next and setseed change it automatically.\
without presets.zip the timer is off and nothing else changes.

the obs text sources for it are in league.json in the MCRL-Streaming repo, inside the p1 to p4 groups. if your scene collection is older, import the new league.json or copy the perch sources over from it. the program moves them into place when it starts.

you can pick which corner of the pov it shows in, in obs_layout.json:\
"perchCorner": "bottom-right"\
it can be top-left, top-right, bottom-left or bottom-right. top-left puts it under the player name. if you leave it out it's bottom-right.
