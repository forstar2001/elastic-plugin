///*
// *    This file is part of ReadonlyREST.
// *
// *    ReadonlyREST is free software: you can redistribute it and/or modify
// *    it under the terms of the GNU General Public License as published by
// *    the Free Software Foundation, either version 3 of the License, or
// *    (at your option) any later version.
// *
// *    ReadonlyREST is distributed in the hope that it will be useful,
// *    but WITHOUT ANY WARRANTY; without even the implied warranty of
// *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// *    GNU General Public License for more details.
// *
// *    You should have received a copy of the GNU General Public License
// *    along with ReadonlyREST.  If not, see http://www.gnu.org/licenses/
// */
//
//package elastic.settings;
//
//
//import java.util.concurrent.*;
//
//public class SettingsPoller {
//  private final SettingsObservable obs;
//  private final Integer subsequentAttemptsIntervalSeconds;
//  private final Boolean forever;
//  private final CompletableFuture<Void> result = new CompletableFuture<>();
//  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
//  private Integer intervalSeconds;
//
//  public SettingsPoller(SettingsObservable obs, Integer intervalSeconds, Integer subsequentAttemptsIntervalSeconds, Boolean forever) {
//    this.obs = obs;
//    this.intervalSeconds = intervalSeconds;
//    this.subsequentAttemptsIntervalSeconds = subsequentAttemptsIntervalSeconds;
//    this.forever = forever;
//
//    // Shut this down otherwise windows won't kill the process.
//  }
//
//  public void poll() {
//
//    // When ReloadableSettings is created at boot time, wait the cluster to stabilise and read in-index settings.
//
//    Runnable job = () -> {
//      if (obs.isClusterReady()) {
//        result.complete(null);
//        try {
//          obs.refreshFromIndex();
//        } catch (Exception e) {
//          if (e.getMessage().equals(obs.SETTINGS_NOT_FOUND_MESSAGE)) {
//            result.complete(null);
//          }
//          else {
//            result.complete(null);
//            throw e;
//          }
//        }
//      }
//      else {
//      }
//    };
//
//    if (System.getProperty("com.readonlyrest.reloadsettingsonboot") == null) {
//      ScheduledFuture scheduledJob = executor
//        .scheduleWithFixedDelay(job, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
//
//      // When the cluster is up, stop polling.
//      result.thenAccept((_x) -> {
//        if (!forever) {
//          scheduledJob.cancel(false);
//          executor.shutdown();
//          return;
//        }
//        intervalSeconds = subsequentAttemptsIntervalSeconds;
//        scheduledJob.cancel(false);
//        executor.scheduleWithFixedDelay(job, subsequentAttemptsIntervalSeconds, subsequentAttemptsIntervalSeconds, TimeUnit.SECONDS);
//      });
//
//    }
//    else {
//      // Never going to complete
//    }
//
//
//  }
//}
