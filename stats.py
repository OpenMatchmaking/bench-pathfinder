# This code is based on the following issue on GitHub:
# https://github.com/docker/docker-py/issues/1795
#
# For using this script you will need
# to install docker-py==1.10.6
import os
import time

from docker import Client


class DockerStats(object):
    CONTAINER_NAME = "pathfinder"
    TEMPLATE = """
    Startup RAM usage (in Mb): {startup_ram}
    Max RAM usage (in Mb): {max_ram}
    Avg RAM usage (in Mb): {avg_ram}
    Max CPU usage (in %): {max_cpu}
    Avg CPU usage (in %): {avg_cpu}
    """

    def __init__(self, *args, **kwargs):
        self.containerName = kwargs.get('container_name', self.CONTAINER_NAME)
        self.resetStatistics()

    def resetStatistics(self):
        self.ramHistory = []
        self.cpuHistory = []

        self.startupRam = 0.0
        self.maxRam = 0.0
        self.avgRam = 0.0
        self.maxCpu = 0.0
        self.avgCpu = 0.0

    def calculateCpuPercent(self, stats):
        cpu_count = len(stats["cpu_stats"]["cpu_usage"]["percpu_usage"])
        cpu_percent = 0.0
        cpu_delta = float(stats["cpu_stats"]["cpu_usage"]["total_usage"]) - float(stats["precpu_stats"]["cpu_usage"]["total_usage"])
        system_delta = float(stats["cpu_stats"]["system_cpu_usage"]) - float(stats["precpu_stats"]["system_cpu_usage"])
        if system_delta > 0.00001:
            cpu_percent = cpu_delta / system_delta * 100.0 * cpu_count
        return cpu_percent

    def calculateCpuStats(self, stats):
        currentCpuUsage = self.calculateCpuPercent(stats)
        self.cpuHistory.append(currentCpuUsage)

        self.maxCpu = max(self.cpuHistory)
        self.avgCpu = sum(self.cpuHistory) / len(self.cpuHistory)

    def calculateRamStats(self, stats):
        currentRamUsage = float(stats["memory_stats"]["usage"]) / 1024 ** 2
        self.ramHistory.append(currentRamUsage)

        self.maxRam = max(self.ramHistory)
        self.avgRam = sum(self.ramHistory) / len(self.ramHistory)

    def collectStatistics(self, timeout=1.0):
        client = Client()

        usedContainerName = None
        for containerInfo in client.containers():
            currentContainerName = containerInfo.get('Names', ['Unknown', ])[0].strip('/')
            currentContainerName = currentContainerName.strip('bench-pathfinder')

            if self.containerName in currentContainerName:
                usedContainerName = containerInfo['Id']
                break

        if not usedContainerName:
            raise ValueError("Container `{}` was not found.".format(self.containerName))

        stats = client.stats(usedContainerName, stream=False)
        self.startupRam = float(stats["memory_stats"]["usage"]) / 1024 ** 2
        self.calculateCpuStats(stats)
        self.calculateRamStats(stats)

        while True:
            stats = client.stats(usedContainerName, stream=False)
            os.system('cls' if os.name == 'nt' else 'clear')
            self.calculateCpuStats(stats)
            self.calculateRamStats(stats)

            print(self.TEMPLATE.format(
                startup_ram=self.startupRam,
                max_ram=self.maxRam,
                avg_ram=self.avgRam,
                max_cpu=self.maxCpu,
                avg_cpu=self.avgCpu
            ))
            time.sleep(timeout)


if __name__ == '__main__':
    stats = DockerStats()
    stats.collectStatistics()
