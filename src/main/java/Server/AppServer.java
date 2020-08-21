package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class AppServer extends Application {
	private TextArea ta = new TextArea();
	private int clientNo = 0;
	double sum = 0;
	private Socket socket = null;
	private ServerSocket serverSocket = null;
	private String[] cmd = { "ls", "pwd", "cd", "mkdir", "rm", "read", "write" };

	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		ScrollPane sp = new ScrollPane();
		sp.setContent(ta);
		sp.setFitToWidth(true);
		ta.setPrefWidth(350);
		ta.setPrefHeight(400);
		Scene scene = new Scene(sp, 350, 500);
		primaryStage.setTitle("Server");
		primaryStage.setScene(scene);
		primaryStage.show();
		new Thread(() -> {
			try {
				serverSocket = new ServerSocket(9000);
				ta.appendText("Started at " + new Date() + '\n');
				while (true) {
					socket = serverSocket.accept();
					Path path = Paths.get(".\\Server").toAbsolutePath();
					System.out.println(path.toString());
					clientNo++;
					Platform.runLater(() -> {
						ta.appendText("Starting thread for Client" + clientNo + new Date() + '\n');
					});
					new Thread(new HandleAClient(socket, path)).start();
				}
			} catch (IOException e) {
				System.err.println(e);
			}
		}).start();
	}

	class HandleAClient implements Runnable {
		private Socket socket;
		private Path path;

		public HandleAClient(Socket socket, Path path) {
			this.socket = socket;
			this.path = path;
		}
		public void run() {
			try {
				DataInputStream fromClient = new DataInputStream(socket.getInputStream());
				DataOutputStream toClient = new DataOutputStream(socket.getOutputStream());
				ObjectOutputStream toClientObj = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream fromClientObj = new ObjectInputStream(socket.getInputStream());
				toClient.writeUTF(path.toString());
				toClient.writeDouble(clientNo);
				while (true) {
					String text = fromClient.readUTF();
					double No = fromClient.readDouble();
					String[] s = text.split(" ");
					if (text.contains("Server")) {
						toClient.writeDouble(1);
						if (s.length > 1) {// control --> Are there errors caused by using space?
							int controlcmd = 0;
							for (int i = 0; i < cmd.length; i++) {
								if (i < 2) {
									if (s[1].equals(cmd[i]))
										controlcmd++;
								} else {
									if (text.contains(cmd[i] + " "))
										controlcmd++;
								}
							}
							if (controlcmd == 1) {// control --> Is the command correct?
								toClient.writeDouble(1);
								toClient.writeUTF("");
								if (s[0].equals(path.toString())) {// path
									path = Paths.get(s[0]);

									System.out.println(path);
									toClient.writeDouble(1);
									if (text.contains("ls")) {
										List<String> fileNames = new ArrayList<>();
										DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path);
										for (Path ap : directoryStream) {
											fileNames.add(ap.subpath(ap.getNameCount() - 1, ap.getNameCount()).toString());
										}
										toClientObj.writeObject(fileNames);
										toClient.writeUTF(path.toString());
									}
									if (text.contains("cd ")) {
										List<String> fileNames = new ArrayList<>();
										DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path);
										for (Path ap : directoryStream) {
											fileNames.add(ap.subpath(ap.getNameCount() - 1, ap.getNameCount()).toString());
										}
										String cd = fromClient.readUTF();
										String[] backdir = path.toString().split("\\\\");
										int whilecontrol = 0;
										String back = "";
										int ifcontrol = 1;
										for (String b : backdir) {
											if (backdir.length > ifcontrol) {
												back = back + b + "\\";
												ifcontrol++;
											}
											if (b.contains(cd))
												whilecontrol = 1;
										}
										System.out.println(cd);
										System.out.println(whilecontrol);
										System.out.println(path.toString().substring(path.toString().length() - 6,path.toString().length()));
										if (cd.equals("..") && !path.toString().substring(path.toString().length() - 6, path.toString().length()).equals("Server")) {// one step back
											toClient.writeDouble(1);
											path = Paths.get(back);
											toClient.writeUTF(path.toString());
										} else if (fileNames.contains(cd)) {// next directory step
											toClient.writeDouble(1);
											path = Paths.get(path + "\\" + cd);
											toClient.writeUTF(path.toString());
										} else if (whilecontrol == 1) {// back directory with given path
											toClient.writeDouble(1);
											int control = 1;
											int j = 0;
											String p = "";
											while (control != 0) {
												p = p + backdir[j] + "\\";
												if (backdir[j].contains(cd)) {
													System.out.println(backdir[j]);
													System.out.println(cd);
													control = 0;
												}
												j++;
											}
											path = Paths.get(p);
											toClient.writeUTF(path.toString());
										} else if (path.toString().substring(path.toString().length() - 6, path.toString().length()).equals("Server")) {
											toClient.writeDouble(2);
											toClient.writeUTF(path.toString());
										} else {// wrong path
											toClient.writeDouble(0);
											toClient.writeUTF(path.toString());
										}
									}
									if (text.contains("pwd")) { // write path for server directory
										toClient.writeUTF(path.toString());
									}
									if (text.contains("mkdir ")) {
										List<String> fileNames = new ArrayList<>();
										DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path);
										for (Path ap : directoryStream) {
											fileNames.add(ap.subpath(ap.getNameCount() - 1, ap.getNameCount()).toString().toUpperCase());
										}
										String newFolder = fromClient.readUTF();
										String newFol = newFolder.toUpperCase();
										int controlmkdir = 1;
										for (int j = 0; j < fileNames.size(); j++) {
											if (fileNames.get(j).equals(newFol)) {
												controlmkdir = 0;
											}
										}
										if (controlmkdir == 0) {
											// not create Folder because same Folder name
											toClient.writeDouble(0);
											toClient.writeUTF(path.toString());
										} else {
											// create Folder
											toClient.writeDouble(1);
											Path newDir = Paths.get(path.toString() + "\\" + newFolder);
											Files.createDirectory(newDir);
											toClient.writeUTF(path.toString());
										}
									}
									if (text.contains("rm ")) {
										List<String> fileNames = new ArrayList<>();
										DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path);
										for (Path ap : directoryStream) {
											fileNames.add(ap.subpath(ap.getNameCount() - 1, ap.getNameCount()).toString().toUpperCase());
										}
										String delFolder = fromClient.readUTF();
										String delFol = delFolder.toUpperCase();
										int control = 0;
										for (int j = 0; j < fileNames.size(); j++) {
											if (fileNames.get(j).equals(delFol)) {
												control = 1;
											}
										}
										if (control == 1) {
											Path delDir = Paths.get(path.toString() + "\\\\" + delFolder);
											File file = new File(delDir.toString());

											if (file.list().length == 0) {// delete Folder for empty
												Files.delete(delDir);
												toClient.writeDouble(1);
											} else {
												// not delete because Folder is not empty, must recursively,
												// First delete the children
												toClient.writeDouble(-1);
											}
											toClient.writeUTF(path.toString());
										} else {// not found deleting Folder
											toClient.writeDouble(0);
											toClient.writeUTF(path.toString());
										}
									}
									if (text.contains("read ")) {
										Path sourcepath = Paths.get(fromClient.readUTF());
										Path destpath = Paths.get(fromClient.readUTF());
										File sfile = new File(sourcepath.toString());
										File dfile = new File(destpath.toString());
										if (sfile.exists() && !dfile.exists() && !sfile.isDirectory() && destpath.toString().contains("Client") && sourcepath.toString().contains("Server")) {
											toClient.writeDouble(1);
											long length = sfile.length();
											toClient.writeDouble(length);
											byte[] bytes = new byte[(int) length];
											FileInputStream in = new FileInputStream(sourcepath.toString());
											bytes = in.readAllBytes();
											toClient.write(bytes);
											toClient.writeUTF("Move the file\n");
										} else if (!destpath.toString().contains("Client")) {
											toClient.writeDouble(0);
											toClient.writeUTF("Destination Path is wrong, Destination path must include Client\n");
										} else if (!sourcepath.toString().contains("Server")) {
											toClient.writeDouble(0);
											toClient.writeUTF("Source path is wrong, Source path must include Server\n");
										} else if (!sfile.exists()) {
											toClient.writeDouble(0);
											toClient.writeUTF("Source path not found\n");
										} else if (dfile.exists()) {
											toClient.writeDouble(0);
											toClient.writeUTF("Destination path is already exist!\n");
										} else if (sfile.isDirectory()) {
											toClient.writeDouble(0);
											toClient.writeUTF("This is folder, Not Moved");
										}
										toClient.writeUTF(path.toString());
									}
									if (text.contains("write ")) {
										Path sourcepath = Paths.get(fromClient.readUTF());
										Path destpath = Paths.get(fromClient.readUTF());
										File sfile = new File(sourcepath.toString());
										File dfile = new File(destpath.toString());
										if (sfile.exists() && !dfile.exists() && !sfile.isDirectory()&& destpath.toString().contains("Server")&& sourcepath.toString().contains("Client")) {
											toClient.writeDouble(1);
											long length = sfile.length();
											toClient.writeDouble(length);
											byte[] bytes = new byte[(int) length];
											FileInputStream in = new FileInputStream(sourcepath.toString());
											bytes = in.readAllBytes();
											toClient.write(bytes);
											toClient.writeUTF("Move the file\n");
										} else if (!destpath.toString().contains("Server")) {
											toClient.writeDouble(0);
											toClient.writeUTF("Destination Path is wrong, Destination path must include Server\n");
										} else if (!sourcepath.toString().contains("Client")) {
											toClient.writeDouble(0);
											toClient.writeUTF("Source path is wrong, Source path must include Client\n");
										} else if (!sfile.exists()) {
											toClient.writeDouble(0);
											toClient.writeUTF("Source path not found\n");
										} else if (dfile.exists()) {
											toClient.writeDouble(0);
											toClient.writeUTF("Destination path is already exist!\n");
										} else if (sfile.isDirectory()) {
											toClient.writeDouble(0);
											toClient.writeUTF("This is folder, Not Moved");
										}
										toClient.writeUTF(path.toString());

									}
								} else {
									// False Path
									toClient.writeDouble(0);
									toClient.writeUTF(path.toString());
								}
							} else {
								toClient.writeDouble(0);
								toClient.writeUTF("This is not recognized as an internal or external command, Please use ls, cd, pwd, mkdir,rm, read and write command. \n");
								toClient.writeUTF(path.toString());
							}
						} else {
							toClient.writeDouble(0);
							toClient.writeUTF("Please leave a space between words and enter the correct command\n");
							toClient.writeUTF(path.toString());
						}
					} else {
						toClient.writeDouble(0);
						toClient.writeUTF(path.toString());
						System.out.println("Out of the Range");
					}
					Platform.runLater(() -> {
						ta.appendText("Client " + (int) No + '\n');
					});
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}