package com.taptap.github.redistools.job;

import com.taptap.github.redistools.config.RedisCopyJobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/12/4
 */
abstract class RedisCopyJobBase {

    private final static Logger logger = LoggerFactory.getLogger(RedisCopyJobBase.class);

    protected static final Set<String> redisReadCommands = new HashSet<>(Arrays.asList(
            "get", "strlen", "exists", "getbit", "getrange", "substr", "mget", "llen", "lindex",
            "lrange", "sismember", "scard", "srandmember", "sinter", "sunion", "sdiff", "smembers",
            "sscan", "zrange", "zrangebyscore", "zrevrangebyscore", "zrangebylex", "zrevrangebylex",
            "zcount", "zlexcount", "zrevrange", "zcard", "zscore", "zrank", "zrevrank", "zscan", "hget",
            "hmget", "hlen", "hstrlen", "hkeys", "hvals", "hgetall", "hexists", "hscan", "randomkey",
            "keys", "scan", "dbsize", "type", "sync", "psync", "ttl", "touch", "pttl", "dump", "object",
            "memory", "bitcount", "bitpos", "georadius_ro", "georadiusbymember_ro", "geohash",
            "geopos", "geodist", "pfcount", "xrange", "xrevrange", "xlen", "xread", "xpending",
            "xinfo", "lolwut"
    ));

    protected static final Set<String> redisWriteCommands = new HashSet<>(Arrays.asList(
            "set", "setnx", "setex", "psetex", "append", "del", "unlink", "setbit", "bitfield", "setrange", "incr",
            "decr", "rpush", "lpush", "rpushx", "lpushx", "linsert", "rpop", "lpop", "brpop", "brpoplpush", "blpop",
            "lset", "ltrim", "lrem", "rpoplpush", "sadd", "srem", "smove", "spop", "sinterstore", "sunionstore",
            "sdiffstore", "zadd", "zincrby", "zrem", "zremrangebyscore", "zremrangebyrank", "zremrangebylex",
            "zunionstore", "zinterstore", "zpopmin", "zpopmax", "bzpopmin", "bzpopmax", "hset", "hsetnx", "hmset",
            "hincrby", "hincrbyfloat", "hdel", "incrby", "decrby", "incrbyfloat", "getset", "mset", "msetnx",
            "swapdb", "move", "rename", "renamenx", "expire", "expireat", "pexpire", "pexpireat", "flushdb",
            "flushall", "sort", "persist", "restore", "restore-asking", "migrate", "bitop", "geoadd", "georadius",
            "georadiusbymember", "pfadd", "pfmerge", "pfdebug", "xadd", "xreadgroup", "xgroup", "xsetid", "xack",
            "xclaim", "xdel", "xtrim"
    ));

    protected static final Set<String> redisOtherCommands = new HashSet<>(Arrays.asList(
            "ping", "info"
    ));

    protected static final Set<String> redisCompressCommands = new HashSet<>(Arrays.asList(
            "set", "setex", "setnx", "hset", "hsetnx"
    ));

    private static final Random random = new Random();

    protected static final Set<String> redisCommands;

    static {
        redisCommands = new HashSet<>();
        redisCommands.addAll(redisReadCommands);
        redisCommands.addAll(redisWriteCommands);
        redisCommands.addAll(redisOtherCommands);
    }

    /**
     * 通用的命令解析
     *
     * @param commandStr 命令字符串
     * @return 命令及参数
     */
    public List<String> parseCommandAndArgs(String commandStr) {
        List<String> commandAndArgs = null;
        try {
            if (commandStr.contains("\\\"")) {
                commandAndArgs = parseCommandAndArgsBySpace(commandStr);
                assert commandAndArgs != null;
                String cmd = commandAndArgs.get(0);
                //存在解析失败的可能性，如果解析出来的指令不在 Redis 指令集，则使用双引号解析在解析一遍
                if (!redisCommands.contains(cmd.toLowerCase())) {
                    commandAndArgs = parseCommandAndArgsByDQ(commandStr);
                }
            } else {
                commandAndArgs = parseCommandAndArgsByDQ(commandStr);
            }
        } catch (Exception e) {
            logger.error("parseCommandAndArgs error, commandStr:{}", commandStr, e);
        }

        if (commandAndArgs == null) {
            logger.info("parseCommandAndArgs error, commandStr:{}", commandStr);
            return null;
        }

        String cmd = commandAndArgs.get(0);
         /*
          单独处理异常结构的命令
         */
        if ("setex".equalsIgnoreCase(cmd) && commandAndArgs.size() > 4) {
            //处理 setex 命令的 value 参数里带有空格的情况
            List<String> tempArgs = commandAndArgs;
            List<String> splitValueStr = tempArgs.subList(3, tempArgs.size() - 1);
            String valueStr = String.join("", splitValueStr);

            commandAndArgs = new ArrayList<>();
            commandAndArgs.add(tempArgs.get(0));
            commandAndArgs.add(tempArgs.get(1));
            commandAndArgs.add(tempArgs.get(2));
            commandAndArgs.add(valueStr);
        }

        return commandAndArgs;
    }

    /**
     * 通用的命令解析，通过双引号解析
     *
     * @param commandStr 命令字符串
     * @return 命令及参数
     */
    private List<String> parseCommandAndArgsByDQ(String commandStr) {
        // 移除时间戳和连接信息
        String[] parts = commandStr.split("\"");
        if (parts.length < 2) {
            return null;
        }
        // 获取命令和参数
        String cmd = parts[1];
        List<String> commandAndArgs = new ArrayList<>();
        commandAndArgs.add(cmd);
        for (int i = 3; i < parts.length; i += 2) {
            commandAndArgs.add(parts[i]);
        }
        return commandAndArgs;
    }

    /**
     * case by case 的处理 redis 的 set 命令，通过空格解析
     *
     * @param commandStr 命令字符串
     * @return 命令及参数
     */
    private List<String> parseCommandAndArgsBySpace(String commandStr) {
        String[] parts = commandStr
                .replace("\\", "")
                .replaceAll("\" \"", " ")
                .split(" ");
        if (parts.length < 2) {
            return null;
        }
        // 获取命令和参数
        String cmd = parts[2].replace("\"", "");
        List<String> args = new ArrayList<>(Arrays.asList(parts).subList(3, parts.length));
        if (!redisCommands.contains(cmd.toLowerCase())) {
            //又是一个特殊的例子
            cmd = parts[3].replace("\"", "");
            args = new ArrayList<>(Arrays.asList(parts).subList(4, parts.length));
        }
        if (cmd.equalsIgnoreCase("set") && args.size() > 4) {
            // 处理 set 命令的 value 参数里带有空格的情况
            List<String> tempArgs = args;
            List<String> splitValueStr = tempArgs.subList(1, tempArgs.size() - 2);
            String valueStr = String.join(" ", splitValueStr);

            args = new ArrayList<>();
            args.add(tempArgs.get(0));
            args.add(valueStr);
            args.addAll(tempArgs.subList(tempArgs.size() - 2, tempArgs.size()));
        }

        if ((cmd.equalsIgnoreCase("hset") || cmd.equalsIgnoreCase("hsetnx")) && args.size() > 3) {
            List<String> tempArgs = args;
            List<String> splitValueStr = tempArgs.subList(2, tempArgs.size());
            String valueStr = String.join(" ", splitValueStr);

            args = new ArrayList<>();
            args.add(tempArgs.get(0));
            args.add(tempArgs.get(1));
            args.add(valueStr);
        }

        String lastArgs = args.remove(args.size() - 1).replace("\"", "").replace("\n", "");
        args.add(lastArgs);

        List<String> commandAndArgs = new ArrayList<>();
        commandAndArgs.add(cmd);
        commandAndArgs.addAll(args);
        return commandAndArgs;
    }

    public int getRandomBatchSize(RedisCopyJobConfig config) {

        int randomNumber = random.nextInt(100); // 生成一个0到99之间的随机数

        if (randomNumber < 10) {
            return config.getMgetKeysCountBuket1();
        } else if (randomNumber < 20) {
            return config.getMgetKeysCountBuket2();
        } else if (randomNumber < 30) {
            return config.getMgetKeysCountBuket3();
        } else if (randomNumber < 40) {
            return config.getMgetKeysCountBuket4();
        } else if (randomNumber < 50) {
            return config.getMgetKeysCountBuket5();
        } else if (randomNumber < 60) {
            return config.getMgetKeysCountBuket6();
        } else if (randomNumber < 70) {
            return config.getMgetKeysCountBuket7();
        } else if (randomNumber < 80) {
            return config.getMgetKeysCountBuket8();
        } else if (randomNumber < 90) {
            return config.getMgetKeysCountBuket9();
        } else {
            return config.getMgetKeysCountBuket10();
        }
    }
}
