package uit.carbon_shop.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uit.carbon_shop.domain.AppUser;
import uit.carbon_shop.domain.CompanyReview;
import uit.carbon_shop.domain.Order;
import uit.carbon_shop.domain.Project;
import uit.carbon_shop.domain.ProjectReview;
import uit.carbon_shop.domain.Question;
import uit.carbon_shop.model.AppUserDTO;
import uit.carbon_shop.model.UserStatus;
import uit.carbon_shop.repos.AppUserRepository;
import uit.carbon_shop.repos.CompanyRepository;
import uit.carbon_shop.repos.CompanyReviewRepository;
import uit.carbon_shop.repos.OrderRepository;
import uit.carbon_shop.repos.ProjectRepository;
import uit.carbon_shop.repos.ProjectReviewRepository;
import uit.carbon_shop.repos.QuestionRepository;
import uit.carbon_shop.util.NotFoundException;
import uit.carbon_shop.util.ReferencedWarning;


@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final CompanyRepository companyRepository;
    private final ProjectRepository projectRepository;
    private final CompanyReviewRepository companyReviewRepository;
    private final ProjectReviewRepository projectReviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUserMapper appUserMapper;
    private final OrderRepository orderRepository;
    private final QuestionRepository questionRepository;
    private final RedissonClient redissonClient;

    public Page<AppUserDTO> findAll(final String filter, final Pageable pageable) {
        Page<AppUser> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = appUserRepository.findAllById(longFilter, pageable);
        } else {
            page = appUserRepository.findAll(pageable);
        }
        return new PageImpl<>(page.getContent()
                .stream()
                .map(appUser -> appUserMapper.updateAppUserDTO(appUser, new AppUserDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public AppUserDTO findByCompany(final Long companyId) {
        return appUserRepository.findByCompany_Id(companyId)
                .map(appUser -> appUserMapper.updateAppUserDTO(appUser, new AppUserDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Page<AppUserDTO> findByStatus(UserStatus status, final Pageable pageable) {
        final Page<AppUser> page = appUserRepository.findByStatus(status, pageable);
        return new PageImpl<>(page.getContent()
                .stream()
                .map(appUser -> appUserMapper.updateAppUserDTO(appUser, new AppUserDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public AppUserDTO get(final Long userId) {
        return appUserRepository.findById(userId)
                .map(appUser -> appUserMapper.updateAppUserDTO(appUser, new AppUserDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final AppUserDTO appUserDTO) {
        final AppUser appUser = new AppUser();
        appUser.setId(appUserDTO.getUserId());
        appUserMapper.updateAppUser(appUserDTO, appUser, companyRepository, projectRepository, companyReviewRepository,
                projectReviewRepository, passwordEncoder);
        return appUserRepository.save(appUser).getId();
    }

    public void update(final Long userId, final AppUserDTO appUserDTO) {
        final AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(NotFoundException::new);
        appUserMapper.updateAppUser(appUserDTO, appUser, companyRepository, projectRepository, companyReviewRepository,
                projectReviewRepository, passwordEncoder);
        appUserRepository.save(appUser);
    }

    public void updateStatus(final Long userId, final UserStatus status) {
        RLock appUserLock = redissonClient.getLock("APP_USER_LOCK:" + userId);
        log.info("Start lock key APP_USER_LOCK:{} on threadId={}", userId, Thread.currentThread().threadId());
        appUserLock.lock();
        log.info("Locked key APP_USER_LOCK:{} on threadId={}", userId, Thread.currentThread().threadId());
        try {
            final AppUser appUser = appUserRepository.findById(userId)
                    .orElseThrow(NotFoundException::new);
            if (!appUser.getStatus().canUpdateTo(status)) {
                throw new IllegalArgumentException(
                        "Cannot update status from " + appUser.getStatus() + " to " + status);
            }
            appUser.setStatus(status);
            if (status == UserStatus.APPROVED) {
                appUser.setApprovedAt(LocalDateTime.now());
            } else if (status == UserStatus.REJECTED) {
                appUser.setRejectedAt(LocalDateTime.now());
            }
            appUserRepository.saveAndFlush(appUser);
        } finally {
            appUserLock.unlock();
            log.info("Unlocked key APP_USER_LOCK:{} on threadId={}", userId, Thread.currentThread().threadId());
        }
    }

    public void approve(final Long userId) {
        updateStatus(userId, UserStatus.APPROVED);
    }

    public void reject(final Long userId) {
        updateStatus(userId, UserStatus.REJECTED);
    }

    public void delete(final Long userId) {
        appUserRepository.deleteById(userId);
    }

    public boolean companyExists(final Long id) {
        return appUserRepository.existsByCompanyId(id);
    }

    public ReferencedWarning getReferencedWarning(final Long userId) {
        final ReferencedWarning referencedWarning = new ReferencedWarning();
        final AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(NotFoundException::new);
        final Project auditByProject = projectRepository.findFirstByAuditBy(appUser);
        if (auditByProject != null) {
            referencedWarning.setKey("appUser.project.auditBy.referenced");
            referencedWarning.addParam(auditByProject.getId());
            return referencedWarning;
        }
        final Order processByOrder = orderRepository.findFirstByProcessBy(appUser);
        if (processByOrder != null) {
            referencedWarning.setKey("appUser.order.processBy.referenced");
            referencedWarning.addParam(processByOrder.getId());
            return referencedWarning;
        }
        final Order createdByOrder = orderRepository.findFirstByCreatedBy(appUser);
        if (createdByOrder != null) {
            referencedWarning.setKey("appUser.order.createdBy.referenced");
            referencedWarning.addParam(createdByOrder.getId());
            return referencedWarning;
        }
        final CompanyReview reviewByCompanyReview = companyReviewRepository.findFirstByReviewBy(appUser);
        if (reviewByCompanyReview != null) {
            referencedWarning.setKey("appUser.companyReview.reviewBy.referenced");
            referencedWarning.addParam(reviewByCompanyReview.getId());
            return referencedWarning;
        }
        final ProjectReview reviewByProjectReview = projectReviewRepository.findFirstByReviewBy(appUser);
        if (reviewByProjectReview != null) {
            referencedWarning.setKey("appUser.projectReview.reviewBy.referenced");
            referencedWarning.addParam(reviewByProjectReview.getId());
            return referencedWarning;
        }
        final Question askedByQuestion = questionRepository.findFirstByAskedBy(appUser);
        if (askedByQuestion != null) {
            referencedWarning.setKey("appUser.question.askedBy.referenced");
            referencedWarning.addParam(askedByQuestion.getId());
            return referencedWarning;
        }
        return null;
    }

}
