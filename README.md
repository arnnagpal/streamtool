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
