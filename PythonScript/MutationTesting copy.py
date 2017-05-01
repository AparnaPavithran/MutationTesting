import os
import shutil
import subprocess
import time

def runfun(cmd):
    #print("runfun fun")
    proc = subprocess.Popen(cmd,stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True )
    outs = "SUCCESS"
    try:
        outs, errs = proc.communicate(timeout=120)
    except TimeoutError:
        proc.kill()
        outs, errs = proc.communicate()
    finally:
        proc.kill()
        #print("runfun fun done")
        return outs


def readfun():
    #print("readfun fun")
    file = open('MutationReport.txt', 'r')
    lines = file.readlines()[0:5]

    # print(lines)
    projName = lines[0].split(";")[1].strip(" \n\t\r")
    projLoc = lines[1].split(";")[1].strip(" \n\t\r")
    mutantProjLoc = lines[4].split(";")[1].strip(" \n\t\r")

    # print mutantProjLoc
    #print("readfun fun done")
    return projName, projLoc, mutantProjLoc


def mutationScorefun():
    #print("mutationScorefun fun")
    total_mutants = 0
    killed_mutants = 0
    print("STAGE 2: *****Executing Test cases*****")
    dirs = os.listdir(mutantProjLoc)
    if len(dirs) > 0 :
        for file in dirs:
            total_mutants += 1
            loc = mutantProjLoc + "/" + file
            print("Running: "+loc)
            os.chdir(loc)
            # print loc
            mvn_test_result = str(runfun(["mvn","test"]))
            #print(mvn_test_result)
            if "Timeout" in mvn_test_result:
                killed_mutants += 1
            if "BUILD FAILURE" in mvn_test_result:
                killed_mutants += 1
            if "SUCCESS" in mvn_test_result:
                killed_mutants += 1
        #print(mvn_test_result)
        live_mutants = total_mutants - killed_mutants
        mutation_score = killed_mutants / total_mutants
        print("Test cases ran successfully")
        print("************************************************\n")
        print("Total Mutants: ", total_mutants)
        print("Killed Mutants: ", killed_mutants)
        print("Live Mutants: ", live_mutants)
        print("Mutation Score: ", mutation_score)

        #print("mutationScorefun fun done")
        return mutation_score
    return -1

def mutantgenfun():
    #print("generatemutants fun")
    print("STAGE 1: Select the project for Mutation Testing")
    firstcmd = "java -jar  ../target/MutationTesting-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
    p = subprocess.Popen(firstcmd, stdout=subprocess.PIPE, shell=True)

    (output, err) = p.communicate()

    # This makes the wait possible
    p_status = p.wait()

    # This will give you the output of the command being executed
    # print output
    print("Generated Mutants successfully")
    print("************************************************\n")
    #print("generatemutants fun done")
    return err

start = time.time()
err = mutantgenfun()

print ("Error: ",err)

projName, projLoc, mutantProjLoc = readfun()

print(mutantProjLoc)

mutation_score = mutationScorefun()

# print(mutantProjLoc)
shutil.rmtree(mutantProjLoc, ignore_errors=True)
#
if mutation_score == -1:
    print("*************************************************************\n")
    print("No project found in "+mutantProjLoc)

print("Project Location: "+projLoc)
print("Mutation Testing Report Location: "+projLoc+"/report")
print("Generated Mutants Location: "+projLoc+"/mutants")
print("Mutated Project Loc"+mutantProjLoc)


print("Time Elapsed: ",time.time()-start)
print("\n************************************************************")
