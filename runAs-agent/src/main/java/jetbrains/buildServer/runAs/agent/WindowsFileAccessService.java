package jetbrains.buildServer.runAs.agent;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.dotNet.buildRunner.agent.CommandLineArgument;
import jetbrains.buildServer.dotNet.buildRunner.agent.CommandLineResource;
import jetbrains.buildServer.dotNet.buildRunner.agent.CommandLineSetup;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.runAs.agent.Constants.ICACLS_TOOL_NAME;

public class WindowsFileAccessService implements FileAccessService {
  private static final Logger LOG = Logger.getInstance(WindowsFileAccessService.class.getName());
  private static final int EXECUTION_TIMEOUT_SECONDS = 600;
  private final CommandLineExecutor myCommandLineExecutor;

  public WindowsFileAccessService(
    @NotNull final CommandLineExecutor commandLineExecutor) {
    myCommandLineExecutor = commandLineExecutor;
  }

  public void setAccess(@NotNull final AccessControlList accessControlList) {
    for (AccessControlEntry entry : accessControlList) {
      applyAccessControlEntryForWindows(entry);
    }
  }

  private void applyAccessControlEntryForWindows(final AccessControlEntry entry) {
    final EnumSet<AccessPermissions> permissions = entry.getPermissions();
    if(permissions.size() == 0) {
      return;
    }

    final AccessControlAccount account = entry.getAccount();
    String username = null;
    switch (account.getTargetType()) {
      case User:
        username = account.getUserName();
        break;

      case All:
        username = "NT AUTHORITY\\Authenticated Users";
        break;
    }

    if(username == null) {
      return;
    }

    String filePath = entry.getFile().getAbsolutePath();

    final ArrayList<CommandLineArgument> args = new ArrayList<CommandLineArgument>();
    args.add(new CommandLineArgument(filePath, CommandLineArgument.Type.PARAMETER));
    args.add(new CommandLineArgument("/C", CommandLineArgument.Type.PARAMETER));
    args.add(new CommandLineArgument("/Q", CommandLineArgument.Type.PARAMETER));

    List<String> grantedPermissionList = new ArrayList<String>();
    if(permissions.contains(AccessPermissions.GrantRead)) {
      grantedPermissionList.add("R");
    }

    if(permissions.contains(AccessPermissions.GrantWrite)) {
      grantedPermissionList.add("W,D,DC");
    }

    if(permissions.contains(AccessPermissions.GrantExecute)) {
      grantedPermissionList.add("RX");
    }

    List<String> deniedPermissionList = new ArrayList<String>();
    if(permissions.contains(AccessPermissions.DenyRead)) {
      deniedPermissionList.add("R");
    }

    if(permissions.contains(AccessPermissions.DenyWrite)) {
      deniedPermissionList.add("W,D,DC");
    }

    if(permissions.contains(AccessPermissions.DenyExecute)) {
      deniedPermissionList.add("X");
    }

    if(grantedPermissionList.size() > 0) {
      args.add(new CommandLineArgument("/grant", CommandLineArgument.Type.PARAMETER));
      final boolean recursive = permissions.contains(AccessPermissions.Recursive);
      final String permissionsStr = username + ":" + (recursive ? "(OI)(CI)" : "") + "(" + StringUtil.join(grantedPermissionList, ",") + ")";
      args.add(new CommandLineArgument(permissionsStr, CommandLineArgument.Type.PARAMETER));
    }

    if(deniedPermissionList.size() > 0) {
      args.add(new CommandLineArgument("/deny", CommandLineArgument.Type.PARAMETER));
      final boolean recursive = permissions.contains(AccessPermissions.Recursive);
      final String permissionsStr = username + ":" + (recursive ? "(OI)(CI)" : "") + "(" + StringUtil.join(deniedPermissionList, ",") + ")";
      args.add(new CommandLineArgument(permissionsStr, CommandLineArgument.Type.PARAMETER));
    }

    final CommandLineSetup icaclsCommandLineSetup = new CommandLineSetup(ICACLS_TOOL_NAME, args, Collections.<CommandLineResource>emptyList());
    try {
      final ExecResult result = myCommandLineExecutor.runProcess(icaclsCommandLineSetup, EXECUTION_TIMEOUT_SECONDS);
      processResult(result);
    }
    catch (ExecutionException e) {
      LOG.error(e);
    }
  }

  private void processResult(final ExecResult result) {
    if(result != null && result.getExitCode() != 0) {
      final String[] outLines = result.getOutLines();
      if(outLines != null && outLines.length > 0) {
        for (String line: outLines) {
          LOG.warn(line);
        }
      }

      final String stderr = result.getStderr();
      if(stderr.length() > 0) {
        LOG.warn(stderr);
      }
    }
  }
}