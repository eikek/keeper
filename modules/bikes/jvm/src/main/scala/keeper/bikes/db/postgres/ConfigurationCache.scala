package keeper.bikes.db.postgres

import java.time.Instant

import keeper.core.TotalsTracker.EntryMap.given
import keeper.core._

import skunk._
import skunk.codec.all as c
import skunk.implicits.*

final case class ConfigurationCache(
    id: ConfigurationCache.Id,
    maintenanceId: MaintenanceId,
    version: Int,
    configuration: DeviceBuild,
    totals: TotalsTracker.EntryMap = Map.empty
):
  def tracker: TotalsTracker =
    new TotalsTracker(configuration, totals)

object ConfigurationCache:
  opaque type Id = Long
  object Id:
    val codec: Codec[Id] = c.int8

  // if the configuration data structure changes in incompatible ways, the version should
  // change so old values are not tried to read
  private val version: Int = 1

  val cfgCodec: Codec[DeviceBuild] = Codecs.jsonb[DeviceBuild]
  val totalsCodec: Codec[TotalsTracker.EntryMap] = Codecs.jsonb

  val codec: Codec[ConfigurationCache] =
    (Id.codec *: Codecs.maintenanceId *: c.int4 *: cfgCodec *: totalsCodec)
      .to[ConfigurationCache]

  val insert: Command[MaintenanceBuild] =
    sql"""
       insert into configuration_cache (maintenance_id, version, configuration, totals_tracker)
       values (${Codecs.maintenanceId}, ${c.int4}, $cfgCodec, $totalsCodec)
       """.command
      .contramap[MaintenanceBuild] { mb =>
        mb.maintenance.id *: version *: mb.build *: mb.tracker.totals *: EmptyTuple
      }

  val findLatest: Query[Void, ConfigurationCache] =
    sql"""
       select c.id, c.maintenance_id, c.version, c.configuration, c.totals_tracker
       from configuration_cache c
       inner join maintenance m on c.maintenance_id = m.id
       where c.version = #${version.toString}
       order by m.date desc
       limit 1
       """
      .query(codec)

  val findLatestBefore: Query[Instant, ConfigurationCache] =
    sql"""
       select c.id, c.maintenance_id, c.version, c.configuration, c.totals_tracker
       from configuration_cache c
       inner join maintenance m on c.maintenance_id = m.id
       where m.date < ${Codecs.instant} and version = #${version.toString}
       order by m.date desc
       limit 1
       """
      .query(codec)
