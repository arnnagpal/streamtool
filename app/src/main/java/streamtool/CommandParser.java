package streamtool;

import org.json.JSONObject;

public class CommandParser {

    Data data;

    public CommandParser(Data data) {
        this.data = data;
    }

    // returns JSONObject with the command data
    // key "invalid": 0 = valid, 1 = unknown command, 2 = syntax error, 3 = null input, 4 = blank input
    // key "command" (0, 2): command as string
    // other data is included in other fields, commonly "value" or "id" if it's a player
    JSONObject getCommand(String command) {

        JSONObject result = new JSONObject();

        if (command == null) {
            result.put("invalid", 3);
            return result;
        }

        String[] splitCommand = command.split(" ");

        if (splitCommand.length == 0) {
            result.put("invalid", 4);
            return result;
        }

        for (int i = 0; i < splitCommand.length; i++) splitCommand[i] = splitCommand[i].replaceAll(" ","");

        String commandType = splitCommand[0].toLowerCase();

        result.put("invalid", 0);

        boolean valid = true;

        switch(commandType) {
            case "quit":
                result.put("command", "quit"); // quit the program
                break;
            case "test":
                result.put("command", "test");
                break;
            case "restore":
                result.put("command", "restore"); // restore previous data
                break;
            case "init":
                result.put("command", "init");
                break;
            case "start":
                result.put("command", "start");
                break;
            case "setinfo":
                result.put("command", "setinfo");
                if (splitCommand.length > 2) {
                    IntParser ip1 = parseInt(splitCommand[1]);
                    IntParser ip2 = parseInt(splitCommand[2]);

                    if (ip1.success && ip2.success && ip1.i > 0 && ip2.i > 0 && ip1.i <= 7) {
                        result.put("lnum", ip1.i);
                        result.put("wnum", ip2.i);
                    } else valid = false;

                } else valid = false;
                break;
            case "killfeeds":
                result.put("command", "killfeeds"); // kill player feeds
                break;
            case "import":
                result.put("command", "import"); // import players
                result.put("value", false);
                if (splitCommand.length > 1) {
                    if (splitCommand[1].toLowerCase().equals("true")) result.put("value", true);
                }
                break;
            case "order", "seedorder":
                result.put("command", "order");
                result.put("value", 0);
                if (splitCommand.length > 1) {
                    if (splitCommand[1].toLowerCase().equals("file")) result.put("value", 1);
                    if (splitCommand[1].toLowerCase().equals("api")) result.put("value", 3);
                    if (splitCommand[1].toLowerCase().equals("show")) result.put("value", 4);
                    if (splitCommand[1].toLowerCase().equals("skip")) {
                        result.put("value", 2);
                        result.put("var", data.currentSeed);
                        if (splitCommand.length > 2) {
                            String s = splitCommand[2];
                            IntParser ip = parseInt(s);
                            if (ip.success && ip.i > 0 && ip.i <= Main.getSeedcount(data.leagueNumber)) {
                                result.put("var", ip.i);
                            }
                        }
                    }
                }
                break;
            case "mute":
                result.put("command", "mute");
                if (splitCommand.length > 1) {
                    if (splitCommand[1].toLowerCase().equals("true")) result.put("value", 1); // true -> mute mic and discord (if enabled)
                    if (splitCommand[1].toLowerCase().equals("false")) result.put("value", 2); // false -> unmute mic and discord (if enabled)
                } else result.put("value", 0); // no argument -> toggle mute (if enabled)
                break;
            case "showtop":
                result.put("command", "showtop");
                if (splitCommand.length > 1) {
                    if (splitCommand[1].toLowerCase().equals("true")) result.put("value", 1); // true -> mute mic and discord (if enabled)
                    if (splitCommand[1].toLowerCase().equals("false")) result.put("value", 2); // false -> unmute mic and discord (if enabled)
                } else result.put("value", 0); // no argument -> toggle mute (if enabled)
                break;
            case "update":
                result.put("command", "update");
                result.put("value", 0); // no argument / unknown argument -> update once
                if (splitCommand.length > 1) {
                    if (splitCommand[1].toLowerCase().equals("auto")) result.put("value", 1); // auto -> enable auto-update
                    if (splitCommand[1].toLowerCase().equals("off") || splitCommand[1].toLowerCase().equals("stop")) result.put("value", 2); // off/stop -> disable auto-update
                    if (splitCommand[1].toLowerCase().equals("random")) result.put("value", 3); // random -> put random povs on the screen
                    if (splitCommand[1].toLowerCase().equals("nomsg")) result.put("value", 4);
                }
                break;
            case "split", "splits", "adv", "advancement":
                result.put("command", "splits"); // print splits
                break;
            case "show":
                result.put("command", "show"); // show a selected player on screen at a selected position
                if (splitCommand.length > 1) {
                    String s = splitCommand[1].toLowerCase();
                    int playerId = identify(s);
                    if (playerId != -1) result.put("id", playerId); else valid = false;
                } else valid = false;

                // use second argument for position if any, default to 1
                result.put("pos", 1);

                if (valid && splitCommand.length > 2) {
                    int pos = -1;
                    String s = splitCommand[2];

                    // use second argument if integer
                    IntParser ip = parseInt(s);
                    if (ip.success) pos = ip.i;

                    // put in the position if it's within range
                    if (pos >= 1 && pos <= 4) {
                        result.put("pos", pos);
                    }
                }

                break;
            case "next":
                result.put("command", "next");
                break;
            case "setseed":
                result.put("command", "setseed");
                if (splitCommand.length > 1) {
                    int seedNumber = -1;
                    String s = splitCommand[1];

                    // check if argument is a number
                    IntParser ip = parseInt(s);
                    if (ip.success) seedNumber = ip.i; else valid = false;

                    // check if the number is within range
                    if (seedNumber < 0 || seedNumber > Main.getSeedcount(data.leagueNumber) + 1) valid = false;

                    result.put("value", seedNumber);
                } else valid = false;
                break;
            case "scene":
                result.put("command", "scene");
                if (splitCommand.length > 1) {
                    String s = splitCommand[1].toLowerCase();
                    switch (s) {
                        case "im", "intermission":
                            result.put("value", "intermission");
                            break;
                        case "ns", "nextseed":
                            result.put("value","nextseed");
                            break;
                        case "sl", "so", "seedlist", "seedorder", "order":
                            result.put("value", "order");
                            break;
                        case "sp", "spec", "spectate", "spectator":
                            result.put("value", "spectator");
                            break;
                        case "pl", "players", "g", "game", "m", "main":
                            result.put("value", "game");
                            result.put("var", 4);
                            if (splitCommand.length > 2) {
                                String s2 = splitCommand[2];
                                IntParser ip = parseInt(s2);
                                if (ip.success && ip.i >= 1 && ip.i <= 4) {
                                    result.put("var", ip.i);
                                }
                            }
                            break;
                        case "update":
                            result.put("value", "update");
                            result.put("var", 4);
                            if (splitCommand.length > 2) {
                                String s2 = splitCommand[2];
                                IntParser ip = parseInt(s2);
                                if (ip.success && ip.i >= 1 && ip.i <= 4) {
                                    result.put("var", ip.i);
                                }
                            }
                            break;
                        case "lb", "leaderboard":
                            result.put("value", "leaderboard");
                            break;
                        case "mw", "multiweek", "avg", "average", "averages":
                            result.put("value", "multiweek");
                            break;
                        case "co", "com", "comm", "comms", "commentators":
                            result.put("value", "commentators");
                            break;
                        case "cm", "comp", "comps", "completes", "completions":
                            result.put("value", "completions");
                            break;
                        case "iv", "interview":
                            result.put("value", "interview");
                            break;
                        default:
                            valid = false;
                            break;
                    }
                } else valid = false;
                break;
            case "interview":
                result.put("command", "interview");
                if (splitCommand.length > 1) {
                    String s = splitCommand[1].toLowerCase();
                    int id = identify(s);
                    if (id != -1) result.put("id", id); else valid = false;
                } else valid = false;
                break;
            case "timer":
                result.put("command", "timer");
                if (splitCommand.length > 1) {
                    String s = splitCommand[1].toLowerCase();
                    switch (s) {
                        case "game", "up":
                            result.put("value", "up");
                            break;
                        case "im", "intermission", "down":
                            result.put("value", "down");
                            if (splitCommand.length > 2) {
                                String s2 = splitCommand[2];
                                IntParser ip = parseInt(s2);
                                if (ip.success && ip.i > 0) {
                                    result.put("var", ip.i);
                                } else valid = false;
                            } else valid = false;
                            break;
                        case "add", "advance":
                            result.put("value", "advance");
                            if (splitCommand.length > 2) {
                                String s2 = splitCommand[2];
                                IntParser ip = parseInt(s2);
                                if (ip.success) {
                                    result.put("var", ip.i);
                                } else valid = false;
                            } else valid = false;
                            break;
                        case "stop":
                            result.put("value", "stop");
                            break;
                        default:
                            valid = false;
                    }
                } else valid = false;
                break;
            case "lb", "leaderboard":
                result.put("command", "leaderboard");

                result.put("value", data.currentSeed);

                if (splitCommand.length > 1) {
                    String s = splitCommand[1];
                    int value = 0;

                    IntParser ip = parseInt(s);
                    if (ip.success) value = ip.i;

                    if (value > 0 && value <= data.currentSeed) {
                        result.put("value", value);
                    }
                }
                break;
            case "comp", "completions":
                result.put("command", "completions");

                result.put("value", 0);

                if (splitCommand.length > 1) {
                    String s = splitCommand[1];
                    int value = -1;

                    IntParser ip = parseInt(s);
                    if (ip.success) value = ip.i;

                    if (value >= 0 && value <= data.currentSeed) {
                        result.put("value", value);
                    }
                }
                break;
            case "match":
                result.put("command", "match");
                if (splitCommand.length > 1) {
                    String value = splitCommand[1].toLowerCase();
                    switch (value) {
                        case "clear", "autodl":
                            result.put("value", value);
                            break;
                        case "id", "getid":
                            result.put("value", "id");
                            result.put("count", 1);
                            if (splitCommand.length > 2) {
                                String s = splitCommand[2];
                                IntParser ip = parseInt(s);
                                if (ip.success && ip.i > 1) result.put("count", ip.i);
                            }
                            break;
                        case "host":
                            result.put("value", "host");
                            if (splitCommand.length > 2) {
                                String name = splitCommand[2];
                                result.put("name", name);
                            } else valid = false;
                            break;
                        case "dl":
                            result.put("value", value);
                            if (splitCommand.length > 3) {
                                String s2 = splitCommand[2];
                                String s3 = splitCommand[3];

                                IntParser ip2 = parseInt(s2);
                                IntParser ip3 = parseInt(s3);

                                if (ip2.success && ip3.success && ip2.i > 0 && ip2.i <= Main.getSeedcount(data.leagueNumber) && ip3.i > 0) {
                                    result.put("seed", ip2.i);
                                    result.put("match", ip3.i);
                                } else valid = false;
                            } else valid = false;
                            break;
                        default:
                            valid = false;
                    }
                } else valid = false;
                break;
            case "setlive":
                result.put("command", "setlive");

                if (splitCommand.length > 1) {
                    String s = splitCommand[1].toLowerCase();
                    int id = identify(s);
                    if (id != -1) result.put("id", id); else valid = false;
                } else valid = false;

                result.put("value", 0);
                if (splitCommand.length > 2) {
                    if (splitCommand[2].toLowerCase().equals("true")) result.put("value", 1);
                    if (splitCommand[2].toLowerCase().equals("false")) result.put("value", 2);
                }
                break;
            case "addplayer":
                result.put("command", "addplayer");
                if (splitCommand.length > 1) {
                    String value = splitCommand[1];
                    result.put("value", value);
                } else valid = false;
                break;

            case "twitch":
                result.put("command", "twitch");
                if (splitCommand.length > 1) {
                    String s = splitCommand[1].toLowerCase();
                    int playerId = identify(s);
                    if (playerId != -1) result.put("id", playerId); else valid = false;
                } else valid = false;

                if (valid && splitCommand.length > 2) {
                    String twitch = splitCommand[2].toLowerCase();
                    result.put("value", twitch);
                }
                break;
            case "list":
                result.put("command", "list");
                break;
            case "live":
                result.put("command", "live");
                break;
            case "hide":
                result.put("command", "hide");
                if (splitCommand.length > 1) {
                    String s = splitCommand[1].toLowerCase();
                    int playerId = identify(s);
                    if (playerId != -1) result.put("id", playerId); else valid = false;
                } else valid = false;
                break;
            case "unhide":
                result.put("command", "unhide");
                break;
            case "reload":
                result.put("command", "reload");
                break;
            case "override":
                result.put("command", "override");
                if (splitCommand.length > 2) {
                    String var = splitCommand[1].toLowerCase();
                    String s = splitCommand[2];

                    IntParser ip = parseInt(s);
                    if (ip.success) {
                        switch (var) {
                            case "promotions", "demotions", "lb1", "lb2", "points", "reset":
                                result.put("var", var);
                                result.put("value", ip.i);
                                break;
                            default:
                                valid = false;
                        }
                    } else if (splitCommand[1].equals("reset")) {
                        result.put("var", "reset");
                    } else valid = false;
                } else if (splitCommand.length > 1 && splitCommand[1].equals("reset")) {
                    result.put("var", "reset");
                } else valid = false;
                break;
            case "snapshot":
                result.put("command", "snapshot");
                if (splitCommand.length > 1) {
                    String s = splitCommand[1].toLowerCase();
                    switch (s) {
                        case "start":
                            result.put("value", 1);
                            break;
                        case "stop":
                            result.put("value", 2);
                            break;
                        case "load":
                            result.put("value", 3);
                            if (splitCommand.length > 2) {
                                String s2 = splitCommand[2];
                                IntParser ip = parseInt(s2);
                                if (ip.success) {
                                    result.put("start", ip.i);
                                } else valid = false;
                            } else valid = false;
                            break;
                        default:
                            result.put("value", 0);
                    }

                } else result.put("value", 0);
                break;
            case "refresh":
                result.put("command","refresh");
                result.put("value", 0);
                if (splitCommand.length > 1) {
                    String s = splitCommand[1];
                    IntParser ip = parseInt(s);
                    if (ip.success) {
                        if (ip.i <= 4 && ip.i >= 1) {
                            result.put("value", ip.i);
                        }
                    }
                }
                break;
            default:
                result.put("invalid", 1);
        }
        
        if (!valid) result.put("invalid", 2);

        return result;
    }

    private IntParser parseInt(String s) {
        IntParser ip = new IntParser();

        try {
            ip.i = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            ip.success = false;
        }

        return ip;
    }

    private int identify(String s) {
        int playerId = -1;
        boolean id = true;

        // check if string is an integer
        IntParser ip = parseInt(s);
        if (ip.success) playerId = ip.i; else id = false;

        // check if any player has the id
        boolean test = false;
        for (int i = 0; i < data.players.length; i++) {
            if (data.players[i].id == playerId) test = true;
        }
        if (!test) id = false;

        if (id) {
            // return playerId if valid
            return playerId;
        } else {
            // see if the string matches any of the player names, and how many
            int count = 0;

            for (int i = 0; i < data.players.length; i++) {
                if (count > 1) break;
                if (data.players[i].name.toLowerCase().contains(s)) {
                    playerId = data.players[i].id;
                    count++;
                }
            }

            // if there's exactly one match, return playerId
            if (count == 1) {
                return playerId;
            } else return -1;
        }
    }
}

class IntParser {
    boolean success = true;
    int i = -1;
}
