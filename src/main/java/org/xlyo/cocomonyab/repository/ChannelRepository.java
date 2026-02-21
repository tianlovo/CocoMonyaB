package org.xlyo.cocomonyab.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.Channel;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Channel entity.
 * Provides CRUD operations and custom query methods for channel management.
 */
@Repository
public interface ChannelRepository extends MongoRepository<Channel, String> {
    
    /**
     * Check if a channel exists by channelId.
     * 
     * @param channelId the Telegram channel ID
     * @return true if a channel with the given channelId exists, false otherwise
     */
    boolean existsByChannelId(Long channelId);
    
    /**
     * Find a channel by channelId.
     * 
     * @param channelId the Telegram channel ID
     * @return an Optional containing the channel if found, empty otherwise
     */
    Optional<Channel> findByChannelId(Long channelId);
    
    /**
     * Find all channels by monitoring status.
     * 
     * @param status the monitoring status
     * @return list of channels with the specified monitoring status
     */
    List<Channel> findByMonitoringStatus(Boolean status);
    
    /**
     * Find channels by username (partial match) and monitoring status with pagination.
     * 
     * @param username the username to search for (partial match)
     * @param status the monitoring status
     * @param pageable pagination information
     * @return page of channels matching the criteria
     */
    Page<Channel> findByChannelUsernameContainingAndMonitoringStatus(
            String username, Boolean status, Pageable pageable);
    
    /**
     * Find channels by username (partial match) with pagination.
     * 
     * @param username the username to search for (partial match)
     * @param pageable pagination information
     * @return page of channels matching the criteria
     */
    Page<Channel> findByChannelUsernameContaining(String username, Pageable pageable);
    
    /**
     * Find channels by monitoring status with pagination.
     * 
     * @param status the monitoring status
     * @param pageable pagination information
     * @return page of channels with the specified monitoring status
     */
    Page<Channel> findByMonitoringStatus(Boolean status, Pageable pageable);
}
