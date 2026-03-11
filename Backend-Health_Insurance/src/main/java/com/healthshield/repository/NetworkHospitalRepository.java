//package com.healthshield.repository;
//
//import com.healthshield.entity.NetworkHospital;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface NetworkHospitalRepository extends JpaRepository<NetworkHospital, Long> {
//    Optional<NetworkHospital> findByHospitalCode(String hospitalCode);
//    List<NetworkHospital> findByCityIgnoreCase(String city);
//    List<NetworkHospital> findByStateIgnoreCase(String state);
//    List<NetworkHospital> findByIsActiveTrue();
//    List<NetworkHospital> findByNabhAccreditedTrue();
//    boolean existsByHospitalCode(String hospitalCode);
//}
