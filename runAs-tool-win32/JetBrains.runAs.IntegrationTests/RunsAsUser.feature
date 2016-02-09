﻿Feature: Runs the command under the specific user account

Scenario: User runs the command under the specific user account
	Given I have appended the file command.cmd by the line WhoAmI.exe
	And I've added the argument -u:TestUser
	And I've added the argument -p:aaa
	And I've added the argument command.cmd
	When I run RunAs tool
	Then the exit code should be 0
	And the output should contain:
	|                |
	| WhoAmI.exe     |
	| .+\\\\TestUser |

Scenario: User runs the command which contains spaces in the path
	Given I have appended the file command Who Am I.cmd by the line WhoAmI.exe
	And I've added the argument -u:TestUser
	And I've added the argument -p:aaa
	And I've added the argument "command Who Am I.cmd"
	When I run RunAs tool
	Then the exit code should be 0
	And the output should contain:
	|                |
	| WhoAmI.exe     |
	| .+\\\\TestUser |

Scenario Outline: User runs the command which contains spaces in the path via config file for args
	Given I have appended the file <cmdFileName> by the line WhoAmI.exe
	And I have appended the file args.txt by the line -u:TestUser
	And I have appended the file args.txt by the line -p:aaa
	And I have appended the file args.txt by the line <cmdFileName>
	And I've added the argument -c:args.txt	
	When I run RunAs tool
	Then the exit code should be 0
	And the output should contain:
	|                |
	| WhoAmI.exe     |
	| .+\\\\TestUser |

Examples:
	| cmdFileName            |
	| command.cmd            |
	| "command.cmd"          |
	| command Who Am I.cmd   |
	| "command Who Am I.cmd" |

Scenario: User runs using config file for args
	Given I have appended the file command.cmd by the line WhoAmI.exe
	And I have appended the file args.txt by the line -p:aaa
	And I have appended the file args.txt by the line command.cmd
	And I've added the argument -u:TestUser
	And I've added the argument -c:args.txt	
	When I run RunAs tool
	Then the exit code should be 0
	And the output should contain:
	|                |
	| WhoAmI.exe     |
	| .+\\\\TestUser |

Scenario: User runs the command with cmd args
	Given I have appended the file command.cmd by the line @echo %1 %2
	And I've added the argument -u:TestUser
	And I've added the argument -p:aaa
	And I've added the argument command.cmd
	And I've added the argument hello
	And I've added the argument "world !!!"
	When I run RunAs tool
	Then the exit code should be 0
	And the output should contain:
	|                      |
	| hello "world !!!" |

Scenario: User runs the command with cmd args via config file for args
	Given I have appended the file command.cmd by the line @echo %1 %~2
	And I have appended the file args.txt by the line command.cmd
	And I have appended the file args.txt by the line hello
	And I have appended the file args.txt by the line "world !!!"
	And I've added the argument -u:TestUser
	And I've added the argument -p:aaa
	And I've added the argument -c:args.txt		
	When I run RunAs tool
	Then the exit code should be 0
	And the output should contain:
	|                  |
	| hello world !!! |

Scenario: User runs the command with cmd args via config file with unquoted args
	Given I have appended the file command.cmd by the line @echo %1 %~2
	And I have appended the file args.txt by the line command.cmd
	And I have appended the file args.txt by the line hello
	And I have appended the file args.txt by the line world !!!
	And I've added the argument -u:TestUser
	And I've added the argument -p:aaa
	And I've added the argument -c:args.txt		
	When I run RunAs tool
	Then the exit code should be 0
	And the output should contain:
	|                  |
	| hello world !!! |

Scenario Outline: RunAs returns the exit code from the target command
	Given I have appended the file command.cmd by the line WhoAmI.exe
	And I have appended the file command.cmd by the line @exit <exitCode> /B
	And I've added the argument -u:TestUser
	And I've added the argument -p:aaa
	And I've added the argument command.cmd
	When I run RunAs tool
	Then the exit code should be <exitCode>
	And the output should contain:
	|                |
	| WhoAmI.exe     |
	| .+\\\\TestUser |

Examples:
	| exitCode  |
	| -99999999 |
	| -99       |
	| 0         |
	| 99        |
	| 99999999  |