package com.auction.common.model.communication;

import java.io.Serializable;

public class Request implements Serializable {
    private CommandType command;
    private Object data;

    public Request(CommandType command, Object data) {
        this.command = command;
        this.data = data;
    }

    public CommandType getCommand() { return command; }
    public Object getData() { return data; }
}