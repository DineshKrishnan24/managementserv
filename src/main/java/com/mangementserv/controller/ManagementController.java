package com.mangementserv.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mangementserv.enumaration.CommandEnum;
import com.mangementserv.enumaration.StatusEnum;
import com.mangementserv.model.Server;
import com.mangementserv.vo.ServerActionVo;
import com.mangementserv.vo.ServerDetailVo;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/management")
public class ManagementController {

    private final static String CONFIG_FILE_PATH = "src/main/resources/server_details.json";

    private List<Server> servers = new ArrayList<>();

    @Bean
    void initializeServers() {
        try {
            servers = new ObjectMapper().readValue(new File(CONFIG_FILE_PATH), List.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/server-action")
    public ResponseEntity<String> restartService(@RequestBody ServerActionVo serverActionVo) {
        Server server = servers.stream().
                filter(eachServer -> eachServer.getName().equalsIgnoreCase(serverActionVo.getName())).
                findFirst().orElse(null);
        if(server != null) {
            String actionCommand = server.getCommands().get(serverActionVo.getAction());
            try {
                executeActionCommand(actionCommand);
                return new ResponseEntity<>("",HttpStatus.ACCEPTED);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return new ResponseEntity<>("Exception while Processing",HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } else {
            return new ResponseEntity<>("Bad Request",HttpStatus.BAD_REQUEST);
        }

    }

    private void executeActionCommand(String actionCommand) throws IOException,InterruptedException {
        Process process = Runtime.getRuntime().exec(actionCommand);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        StringBuilder allLines = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            allLines.append(line);
        }
        int exitVal = process.waitFor();
        if (exitVal != 0 || !new String(allLines).contains("New PID")) {
            throw new IOException("No new Process Id created");
        }
    }

    @GetMapping("/servers")
    public ResponseEntity<List<ServerDetailVo>> getServers() {
        List<ServerDetailVo> serverList = new ArrayList<>();
       servers.parallelStream().forEach(server -> {
           ServerDetailVo serverDetails= new ServerDetailVo();
           try {
               String runningPid = getRunningProcessId(server);
               StatusEnum status = checkProcessRunning(server,runningPid);
               serverDetails.setName(server.getName());
               serverDetails.setStatus(status);
               serverList.add(serverDetails);
           } catch (IOException | InterruptedException e) {
                e.printStackTrace();
           }
       });
       return new ResponseEntity<>(serverList, HttpStatus.OK);
    }

    private StatusEnum checkProcessRunning(Server server, String runningPid) throws IOException {
        String command = server.getCommands().get(CommandEnum.CHECK)+runningPid;
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if(line.contains(runningPid))
                return StatusEnum.RUNNING;
        }
        return StatusEnum.STOPPED;
    }

    private String getRunningProcessId(Server server) throws IOException,InterruptedException {
        String command = "cat /arka-sandbox/" + server.getSnapShotName() +"/RUNNING_PID";
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        String runningPid = reader.readLine();;
        int exitVal = process.waitFor();
        if (exitVal == 0) {
            return runningPid;
        }
        return "";
    }
}
